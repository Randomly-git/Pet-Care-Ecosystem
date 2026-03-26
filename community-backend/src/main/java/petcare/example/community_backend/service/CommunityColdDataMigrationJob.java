package petcare.example.community_backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate newTransactionTemplate;

    private final PetMomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CommunityHBaseColdStorageService hBaseService;
    private final UserServiceFacade userServiceFacade;

    // 迁移开关（可通过配置或接口控制）
    private final AtomicBoolean migrationEnabled = new AtomicBoolean(true);

    /**
     * 设置迁移开关状态
     */
    public void setMigrationEnabled(boolean enabled) {
        this.migrationEnabled.set(enabled);
    }

    /**
     * 获取迁移开关状态
     */
    public boolean isMigrationEnabled() {
        return this.migrationEnabled.get();
    }

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
     * 执行迁移主流程
     */
    @Transactional
    public MigrationStats executeMigration() {
        long startTime = System.currentTimeMillis();
        MigrationStats stats = new MigrationStats();

        log.info("【冷迁移】开始扫描待迁移动态...");

        // 1. 查询待迁移的动态
        // NONE 状态：需要同时满足 7 天无访问条件
        // MIGRATING 状态：表示上次迁移失败遗留的数据，继续处理（不检查 lastAccessTime）
        LocalDateTime threshold = LocalDateTime.now().minusDays(7);
        List<PetMoment> noneStatusMoments = momentRepository.findByMigrationStatusAndLastAccessTimeBefore("NONE", threshold);
        List<PetMoment> stuckMoments = momentRepository.findByMigrationStatus("MIGRATING");

        // 合并两个列表
        List<PetMoment> coldMoments = new ArrayList<>();
        coldMoments.addAll(noneStatusMoments);
        coldMoments.addAll(stuckMoments);

        log.info("【冷迁移】找到 {} 条待迁移动态 (NONE={}, MIGRATING={})",
                coldMoments.size(), noneStatusMoments.size(), stuckMoments.size());

        // 释放持久化上下文，避免实体与原事务绑定
        entityManager.flush();
        entityManager.clear();

        // 提取所有需要迁移的 momentId
        List<Long> momentIds = coldMoments.stream()
                .map(PetMoment::getId)
                .collect(Collectors.toList());

        for (Long momentId : momentIds) {
            // 使用 TransactionTemplate 为每个动态的迁移创建独立事务
            // PROPAGATION_REQUIRES_NEW 确保每个迁移在独立事务中执行
            // 这样即使某个迁移失败，也不会影响其他迁移和外层事务
            Boolean success = newTransactionTemplate.execute(status -> {
                try {
                    return migrateSingleMoment(momentId);
                } catch (Exception e) {
                    log.error("【冷迁移】迁移动态失败: momentId={}, error={}", momentId, e.getMessage(), e);
                    status.setRollbackOnly();
                    return false;
                }
            });

            if (Boolean.TRUE.equals(success)) {
                stats.incrementSuccess();
            } else {
                stats.incrementSkipped();
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
     * 在独立事务中执行，确保每个动态的迁移互不影响
     *
     * 流程：
     * 1. 查询动态（在新事务中重新加载）
     * 2. 尝试设置 MIGRATING 状态（原子操作）
     * 3. 检查评论数量阈值
     * 4. 查询关联数据
     * 5. 构建扁平化归档数据
     * 6. 写入 HBase
     * 7. 删除 MySQL 数据
     */
    @Transactional
    public boolean migrateSingleMoment(Long momentId) {
        log.info("【冷迁移】开始处理动态: momentId={}", momentId);

        // 在当前事务中重新查询动态，避免与查询阶段的事务混淆
        PetMoment moment = momentRepository.findById(momentId).orElse(null);
        if (moment == null) {
            log.warn("【冷迁移】动态不存在: momentId={}", momentId);
            return false;
        }

        Long userId = moment.getUserId();
        String currentStatus = moment.getMigrationStatus();
        log.info("【冷迁移】查询到动态: momentId={}, userId={}, status={}, lastAccessTime={}",
                momentId, userId, currentStatus, moment.getLastAccessTime());

        // ========== 步骤 1：在状态改变之前检查 lastAccessTime ==========
        // 如果用户最近访问过（lastAccessTime 被更新），则不进行迁移
        // 这个检查必须在状态改变之前，确保迁移开始前一刻数据仍然是冷的
        LocalDateTime coldThreshold = LocalDateTime.now().minusDays(7);
        if (moment.getLastAccessTime() != null && moment.getLastAccessTime().isAfter(coldThreshold)) {
            log.info("【冷迁移】动态最近被访问过，跳过迁移: momentId={}, lastAccessTime={}",
                    momentId, moment.getLastAccessTime());
            return false;
        }

        // ========== 步骤 1.5：状态机锁定 ==========
        // 如果状态已经是 MIGRATING，说明是上次迁移失败遗留的数据，直接继续执行迁移
        // 如果状态是 NONE，尝试获取锁
        boolean isResumingFromFailure = "MIGRATING".equals(currentStatus);
        if (!isResumingFromFailure) {
            // 使用乐观锁：只有状态为 NONE 时才能设置为 MIGRATING
            int updated = momentRepository.updateMigrationStatus(momentId, "NONE", "MIGRATING");
            log.info("【冷迁移】尝试获取锁: momentId={}, updated={}", momentId, updated);
            if (updated == 0) {
                log.warn("【冷迁移】动态状态不是 NONE 或 MIGRATING，跳过: momentId={}, status={}", momentId, currentStatus);
                return false;
            }
        } else {
            log.info("【冷迁移】检测到上次迁移失败的遗留数据，继续迁移: momentId={}", momentId);
        }

        // 状态已经锁定为 MIGRATING，后续步骤中即使 lastAccessTime 变化也不检查了
        // 因为状态锁定本身就表示"我正在处理这个"，不应该被用户访问打断

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
                .content(moment.getContent())
                .createdAt(moment.getCreatedAt())
                .build();

        // ========== 步骤 5：写入 HBase（幂等检查） ==========
        // 检查是否已存在（幂等性）
        if (hBaseService.exists(userId, momentId)) {
            log.info("【冷迁移】HBase中已存在数据（幂等跳过）: momentId={}", momentId);
        } else {
            // 写入 HBase
            log.debug("【冷迁移】写入 HBase: momentId={}", momentId);
            hBaseService.saveArchive(archiveRecord, archiveData);

            // 验证 HBase 写入成功
            if (!hBaseService.exists(userId, momentId)) {
                log.error("【冷迁移】HBase写入验证失败: momentId={}", momentId);
                // 恢复状态
                momentRepository.updateMigrationStatus(momentId, "MIGRATING", "NONE");
                throw new RuntimeException("HBase写入验证失败");
            }
            log.debug("【冷迁移】HBase写入验证通过: momentId={}", momentId);
        }

        // ========== 步骤 6：删除 MySQL 数据 ==========
        log.info("【冷迁移】开始删除MySQL数据: momentId={}", momentId);
        deleteFromMySql(momentId, comments, momentLikes, commentLikes);

        log.info("【冷迁移】动态迁移成功: momentId={}, 评论数={}, 点赞数={}",
                momentId, comments.size(), archivedLikes.size());

        return true;
    }

    /**
     * 删除 MySQL 中的数据（按正确顺序，确保外键约束不被违反）
     */
    private void deleteFromMySql(Long momentId, List<Comment> comments,
                                  List<Like> momentLikes, List<Like> commentLikes) {
        log.debug("【冷迁移】开始删除 MySQL 数据: momentId={}", momentId);

        // 1. 删除评论的点赞（先删子表）
        for (Comment comment : comments) {
            likeRepository.deleteByTargetTypeAndTargetId(TargetType.COMMENT, comment.getId());
        }

        // 2. 删除评论
        commentRepository.deleteByMomentId(momentId);

        // 3. 删除动态的点赞
        likeRepository.deleteByTargetTypeAndTargetId(TargetType.MOMENT, momentId);

        // 4. 删除动态（最后删主表）
        momentRepository.deleteById(momentId);

        // 5. 显式 flush，确保所有删除操作立即执行
        entityManager.flush();

        // 6. 验证删除成功
        if (momentRepository.existsById(momentId)) {
            throw new RuntimeException("动态删除失败: momentId=" + momentId);
        }

        log.debug("【冷迁移】MySQL 数据删除完成: momentId={}", momentId);
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
