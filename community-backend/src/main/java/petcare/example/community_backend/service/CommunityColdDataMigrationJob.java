package petcare.example.community_backend.service;

import petcare.example.community_backend.config.HBaseProperties;
import petcare.example.community_backend.model.Comment;
import petcare.example.community_backend.model.Like;
import petcare.example.community_backend.model.PetMoment;
import petcare.example.community_backend.model.TargetType;
import petcare.example.community_backend.repository.CommentRepository;
import petcare.example.community_backend.repository.LikeRepository;
import petcare.example.community_backend.repository.PetMomentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 社区模块冷数据迁移定时任务
 * 
 * 设计原则：
 * 1. 只保留 NONE/MIGRATING 两种状态
 * 2. 迁移成功后删除 MySQL 记录
 * 3. MQ 负责重试，不需要 MySQL 记录重试次数
 * 
 * 执行流程：
 * 1. 扫描 MySQL 中超过7天未访问的动态
 * 2. 发布迁移任务到 MQ（级联发布 comments 和 likes）
 * 3. MQ 消费者执行实际迁移（HBase写入 + MySQL删除）
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityColdDataMigrationJob {

    private final PetMomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CommunityColdStorageEventPublisher eventPublisher;
    private final CommunityHBaseColdStorageService hBaseService;

    @Value("${hbase.cold-data.community.days-threshold:7}")
    private int daysThreshold;

    @Value("${hbase.cold-data.community.batch-size:1000}")
    private int batchSize;

    /**
     * 每天北京时间 3:00 执行冷数据迁移任务
     * 
     * cron表达式说明：
     * - 秒(0) 分(0) 时(3) 日(*) 月(*) 周(?)
     * - 北京时间3:00 = UTC 19:00
     */
    @Scheduled(cron = "${hbase.cold-data.community.migration-cron:0 0 3 * * ?}", zone = "Asia/Shanghai")
    @Transactional
    public void migrateToColdStorage() {
        log.info("================= 社区动态冷数据迁移任务开始 =================");

        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);

        // 1. 处理新发现的待迁移记录（状态为 NONE）
        int newPublished = scanAndPublishMigrationTasks(threshold);

        // 2. 处理之前卡住的记录（状态为 MIGRATING，可能是 MQ 消费失败）
        int stuckResolved = resolveStuckMigrations();

        log.info("================= 社区动态冷数据迁移任务完成 =================");
        log.info("新增发布迁移任务数: {}, 处理卡住记录数: {}", newPublished, stuckResolved);
    }

    /**
     * 扫描并发布迁移任务（状态为 NONE 的记录）
     */
    private int scanAndPublishMigrationTasks(LocalDateTime threshold) {
        log.info("【新迁移】开始扫描待迁移记录，超过 {} 天未访问", daysThreshold);

        int publishedCount = 0;
        int pageNumber = 0;

        while (true) {
            // 分页查询待迁移的记录（状态为 NONE 且超过7天未访问）
            Page<PetMoment> page = momentRepository
                    .findRecordsToMigrate(threshold, PageRequest.of(pageNumber, batchSize));

            if (page.isEmpty()) {
                break;
            }

            for (PetMoment moment : page.getContent()) {
                try {
                    // 收集关联的评论ID和点赞ID
                    List<Long> commentIds = collectCommentIds(moment.getId());
                    List<Long> likeIds = collectLikeIds(moment.getId());

                    // 发布迁移消息到 MQ
                    eventPublisher.publishMigrateToColdEvent(
                            moment.getId(),
                            moment.getUserId(),
                            commentIds,
                            likeIds
                    );

                    // 更新状态为迁移中
                    moment.setMigrationStatus("MIGRATING");
                    momentRepository.save(moment);
                    publishedCount++;

                } catch (Exception e) {
                    log.error("【新迁移】发送迁移任务失败: momentId={}, error={}",
                            moment.getId(), e.getMessage());
                }
            }

            if (!page.hasNext()) {
                break;
            }
            pageNumber++;
        }

        log.info("【新迁移】完成，共发布 {} 条迁移任务", publishedCount);
        return publishedCount;
    }

    /**
     * 处理卡住的迁移记录（状态为 MIGRATING）
     */
    private int resolveStuckMigrations() {
        log.info("【卡住处理】开始处理卡住的迁移记录");

        // 查询所有状态为 MIGRATING 的记录
        Page<PetMoment> stuckPage = momentRepository
                .findMigratingRecords(PageRequest.of(0, batchSize));

        int resolvedCount = 0;

        for (PetMoment moment : stuckPage.getContent()) {
            try {
                // 生成 RowKey
                String rowKey = hBaseService.generateMomentRowKey(
                        moment.getUserId(),
                        moment.getCreatedAt(),
                        moment.getId()
                );

                // 检查 HBase 是否已存在
                if (hBaseService.existsInColdStorage("community_moments", rowKey)) {
                    // HBase 已有数据，说明迁移已完成，直接删除 MySQL 记录
                    log.info("【卡住处理】HBase已存在，直接删除MySQL记录: momentId={}, rowKey={}",
                            moment.getId(), rowKey);
                    deleteMomentAndRelated(moment);
                } else {
                    // HBase 没有数据，需要重新写入
                    log.info("【卡住处理】HBase不存在，重新迁移: momentId={}, rowKey={}",
                            moment.getId(), rowKey);

                    // 发布新的迁移事件
                    List<Long> commentIds = collectCommentIds(moment.getId());
                    List<Long> likeIds = collectLikeIds(moment.getId());

                    eventPublisher.publishMigrateToColdEvent(
                            moment.getId(),
                            moment.getUserId(),
                            commentIds,
                            likeIds
                    );
                }
                resolvedCount++;

            } catch (Exception e) {
                log.error("【卡住处理】处理卡住记录失败: momentId={}, error={}",
                        moment.getId(), e.getMessage());
            }
        }

        log.info("【卡住处理】完成，共处理 {} 条卡住记录", resolvedCount);
        return resolvedCount;
    }

    /**
     * 收集动态关联的评论ID
     */
    private List<Long> collectCommentIds(Long momentId) {
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        return comments.stream()
                .map(Comment::getId)
                .toList();
    }

    /**
     * 收集动态关联的点赞ID
     * 包括动态本身的点赞和评论的点赞
     */
    private List<Long> collectLikeIds(Long momentId) {
        Set<Long> likeIds = new HashSet<>();

        // 收集动态的点赞ID
        List<Like> momentLikes = likeRepository.findByTargetTypeAndTargetId(TargetType.MOMENT, momentId);
        momentLikes.forEach(like -> likeIds.add(like.getId()));

        // 收集评论的点赞ID
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        for (Comment comment : comments) {
            List<Like> commentLikes = likeRepository.findByTargetTypeAndTargetId(
                    TargetType.COMMENT, comment.getId());
            commentLikes.forEach(like -> likeIds.add(like.getId()));
        }

        return likeIds.stream().toList();
    }

    /**
     * 删除动态及其关联的评论和点赞
     */
    private void deleteMomentAndRelated(PetMoment moment) {
        Long momentId = moment.getId();

        // 1. 查询并删除评论的点赞
        List<Comment> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId);
        for (Comment comment : comments) {
            likeRepository.deleteByTargetTypeAndTargetId(TargetType.COMMENT, comment.getId());
        }

        // 2. 删除评论
        commentRepository.deleteByMomentId(momentId);

        // 3. 删除动态的点赞
        likeRepository.deleteByTargetTypeAndTargetId(TargetType.MOMENT, momentId);

        // 4. 删除动态本身
        momentRepository.delete(moment);
    }

    /**
     * 手动触发迁移任务（用于测试或紧急迁移）
     */
    public void manualMigrate() {
        log.info("手动触发冷数据迁移任务");
        migrateToColdStorage();
    }

    /**
     * 获取迁移统计信息
     */
    public MigrationStats getMigrationStats() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(daysThreshold);

        return new MigrationStats(
                momentRepository.count(),
                momentRepository.countPendingMigrationRecords(threshold),
                momentRepository.countMigratingRecords()
        );
    }

    /**
     * 迁移统计信息
     */
    public record MigrationStats(
            long totalRecords,
            long pendingRecords,
            long migratingRecords
    ) {}
}
