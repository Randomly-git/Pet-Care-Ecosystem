package petcare.example.community_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petcare.example.community_backend.client.UserServiceFacade;
import petcare.example.community_backend.client.dto.UserResponseDTO;
import petcare.example.community_backend.dto.ColdArchiveData;
import petcare.example.community_backend.dto.HBaseArchiveRecord;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 社区模块冷数据迁移任务（扁平化单表 + 状态机锁定设计）
 *
 * 设计原则：
 * 1. 扁平化归档：评论和点赞序列化到 full_data 列
 * 2. 状态机锁定：使用 MIGRATING 状态防止并发问题
 * 3. 幂等性：先写 HBase，再删 MySQL
 *
 * 迁移流程：
 * 1. 查询 7 天无访问的动态
 * 2. 设置 MIGRATING 状态（锁定）
 * 3. 查询关联的评论和点赞
 * 4. 构建扁平化归档数据（JSON）
 * 5. 写入 HBase（幂等检查）
 * 6. 验证 HBase 写入成功
 * 7. 删除 MySQL 数据
 *
 * 安全措施：
 * - MIGRATING 状态防止级联删除误杀新数据
 * - 先写后删保证数据不丢失
 * - 批量操作减少事务时间
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityColdDataMigrationJob implements ApplicationRunner {

    private final PetMomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CommunityHBaseColdStorageService hBaseService;
    private final UserServiceFacade userServiceFacade;

    // 迁移开关（可通过配置或接口控制）
    private final AtomicBoolean migrationEnabled = new AtomicBoolean(true);

    // 最大评论数阈值（超过则跳过迁移）
    private static final int MAX_COMMENT_THRESHOLD = 2000;

    /**
     * 启动时执行一次
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("【冷迁移】社区模块冷数据迁移任务已启动");
        // 可选：启动时执行一次迁移
        // executeMigration();
    }

    /**
     * 定时任务：每天凌晨 2:00 执行
     * 使用 cron 表达式可根据实际情况调整
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledMigration() {
        if (!migrationEnabled.get()) {
            log.info("【冷迁移】迁移任务已禁用，跳过执行");
            return;
        }

        log.info("【冷迁移】定时任务开始执行");
        executeMigration();
    }

    /**
     * 执行迁移
     */
    public MigrationStats executeMigration() {
        long startTime = System.currentTimeMillis();
        MigrationStats stats = new MigrationStats();

        log.info("【冷迁移】开始扫描待迁移动态...");

        // 1. 查询 7 天无访问的动态
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<PetMoment> coldMoments = momentRepository.findByMigrationStatusAndLastAccessTimeBefore("NONE", threshold);

        log.info("【冷迁移】找到 {} 条待迁移动态", coldMoments.size());

        for (PetMoment moment : coldMoments) {
            try {
                boolean success = migrateSingleMoment(moment);
                if (success) {
                    stats.incrementSuccess();
                } else {
                    stats.incrementSkipped();
                }
            } catch (Exception e) {
                log.error("【冷迁移】迁移动态失败: momentId={}, error={}", moment.getId(), e.getMessage(), e);
                stats.incrementFailed();
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        stats.setElapsedMs(elapsed);

        log.info("【冷迁移】迁移任务完成: 成功={}, 跳过={}, 失败={}, 耗时={}ms",
                stats.getSuccess(), stats.getSkipped(), stats.getFailed(), elapsed);

        return stats;
    }

    /**
     * 迁移单个动态（状态机锁定 + 扁平化归档）
     *
     * 流程：
     * 1. 尝试设置 MIGRATING 状态（原子操作）
     * 2. 检查评论数量阈值
     * 3. 查询关联数据
     * 4. 构建归档数据
     * 5. 写入 HBase
     * 6. 删除 MySQL 数据
     */
    @Transactional
    public boolean migrateSingleMoment(PetMoment moment) {
        Long momentId = moment.getId();
        Long userId = moment.getUserId();

        log.debug("【冷迁移】开始处理动态: momentId={}, userId={}", momentId, userId);

        // ========== 步骤 1：状态机锁定 ==========
        // 使用乐观锁：只有状态为 NONE 时才能设置为 MIGRATING
        int updated = momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING");
        if (updated == 0) {
            log.debug("【冷迁移】动态状态不是 NONE，可能正在被处理: momentId={}", momentId);
            return false;
        }

        // 重新查询最新状态
        PetMoment currentMoment = momentRepository.findById(momentId).orElse(null);
        if (currentMoment == null) {
            log.warn("【冷迁移】动态不存在: momentId={}", momentId);
            return false;
        }

        // 再次检查状态
        if (!"MIGRATING".equals(currentMoment.getMigrationStatus())) {
            log.debug("【冷迁移】动态状态已变更: momentId={}, status={}", momentId, currentMoment.getMigrationStatus());
            return false;
        }

        // ========== 步骤 2：检查评论数量阈值 ==========
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        if (comments.size() > MAX_COMMENT_THRESHOLD) {
            log.info("【冷迁移】动态评论数超过阈值，跳过迁移: momentId={}, commentCount={}, threshold={}",
                    momentId, comments.size(), MAX_COMMENT_THRESHOLD);
            // 恢复状态
            momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE");
            return false;
        }

        // ========== 步骤 3：查询关联数据 ==========
        // 查询动态的点赞
        List<Like> momentLikes = likeRepository.findByTargetTypeAndTargetId(TargetType.MOMENT, momentId);

        // 查询评论的点赞
        List<Like> commentLikes = new ArrayList<>();
        Set<Long> commentIds = comments.stream().map(Comment::getId).collect(Collectors.toSet());
        if (!commentIds.isEmpty()) {
            commentLikes = likeRepository.findByTargetTypeAndTargetIdIn(TargetType.COMMENT, commentIds);
        }

        // 获取评论用户信息
        List<Long> commentUserIds = comments.stream().map(Comment::getUserId).collect(Collectors.toList());
        List<Long> likeUserIds = momentLikes.stream().map(Like::getUserId).collect(Collectors.toSet())
                .stream()
                .collect(Collectors.toList());
        commentUserIds.addAll(likeUserIds);
        Set<Long> allUserIds = commentUserIds.stream().collect(Collectors.toSet());

        java.util.Map<Long, UserResponseDTO> userMap = userServiceFacade.batchGetUsers(allUserIds);

        // ========== 步骤 4：构建扁平化归档数据 ==========
        // 构建评论归档数据
        List<ColdArchiveData.ArchivedComment> archivedComments = comments.stream()
                .map(c -> {
                    UserResponseDTO u = userMap.get(c.getUserId());
                    return ColdArchiveData.ArchivedComment.builder()
                            .commentId(c.getId())
                            .momentId(c.getMomentId())
                            .userId(c.getUserId())
                            .content(c.getContent())
                            .parentId(c.getParentId())
                            .userName(u != null ? u.getNickname() : "")
                            .userAvatar(u != null ? u.getAvatarUrl() : "")
                            .createdAt(c.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());

        // 构建点赞归档数据
        List<ColdArchiveData.ArchivedLike> archivedLikes = new ArrayList<>();

        // 动态点赞
        for (Like like : momentLikes) {
            UserResponseDTO u = userMap.get(like.getUserId());
            archivedLikes.add(ColdArchiveData.ArchivedLike.builder()
                    .likeId(like.getId())
                    .userId(like.getUserId())
                    .targetType(ColdArchiveData.ArchivedLikeTargetType.MOMENT)
                    .targetId(like.getTargetId())
                    .userName(u != null ? u.getNickname() : "")
                    .userAvatar(u != null ? u.getAvatarUrl() : "")
                    .createdAt(like.getCreatedAt())
                    .build());
        }

        // 评论点赞
        for (Like like : commentLikes) {
            UserResponseDTO u = userMap.get(like.getUserId());
            archivedLikes.add(ColdArchiveData.ArchivedLike.builder()
                    .likeId(like.getId())
                    .userId(like.getUserId())
                    .targetType(ColdArchiveData.ArchivedLikeTargetType.COMMENT)
                    .targetId(like.getTargetId())
                    .userName(u != null ? u.getNickname() : "")
                    .userAvatar(u != null ? u.getAvatarUrl() : "")
                    .createdAt(like.getCreatedAt())
                    .build());
        }

        // 构建归档元数据
        ColdArchiveData archiveData = ColdArchiveData.builder()
                .comments(archivedComments)
                .likes(archivedLikes)
                .metadata(ColdArchiveData.ArchiveMetadata.builder()
                        .commentCount(comments.size())
                        .likeCount(archivedLikes.size())
                        .snapshotTime(LocalDateTime.now())
                        .build())
                .build();

        // 构建归档记录
        HBaseArchiveRecord archiveRecord = HBaseArchiveRecord.builder()
                .momentId(momentId)
                .userId(userId)
                .content(currentMoment.getContent())
                .createdAt(currentMoment.getCreatedAt())
                .build();

        // ========== 步骤 5：写入 HBase（幂等检查） ==========
        // 检查是否已存在（幂等性）
        if (hBaseService.exists(userId, momentId)) {
            log.info("【冷迁移】HBase中已存在数据（幂等跳过）: momentId={}", momentId);
            // 直接删除 MySQL 数据
            deleteFromMySql(momentId, comments, momentLikes, commentLikes);
            return true;
        }

        // 写入 HBase
        hBaseService.saveArchive(archiveRecord, archiveData);

        // 验证 HBase 写入成功
        if (!hBaseService.exists(userId, momentId)) {
            log.error("【冷迁移】HBase写入验证失败: momentId={}", momentId);
            // 恢复状态
            momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE");
            throw new RuntimeException("HBase写入验证失败");
        }

        // ========== 步骤 6：删除 MySQL 数据 ==========
        deleteFromMySql(momentId, comments, momentLikes, commentLikes);

        log.info("【冷迁移】动态迁移成功: momentId={}, 评论数={}, 点赞数={}",
                momentId, comments.size(), archivedLikes.size());

        return true;
    }

    /**
     * 删除 MySQL 中的数据
     */
    private void deleteFromMySql(Long momentId, List<Comment> comments,
                                  List<Like> momentLikes, List<Like> commentLikes) {
        // 删除评论的点赞
        for (Comment comment : comments) {
            likeRepository.deleteByTargetTypeAndTargetId(TargetType.COMMENT, comment.getId());
        }

        // 删除评论
        commentRepository.deleteByMomentId(momentId);

        // 删除动态的点赞
        likeRepository.deleteByTargetTypeAndTargetId(TargetType.MOMENT, momentId);

        // 删除动态
        momentRepository.deleteById(momentId);
    }

    /**
     * 迁移统计
     */
    @lombok.Data
    public static class MigrationStats {
        private int success = 0;
        private int skipped = 0;
        private int failed = 0;
        private long elapsedMs = 0;

        public void incrementSuccess() { success++; }
        public void incrementSkipped() { skipped++; }
        public void incrementFailed() { failed++; }
    }
}
