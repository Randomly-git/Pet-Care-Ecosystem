package petcare.example.community_backend.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import petcare.example.community_backend.config.RabbitMQConfig;
import petcare.example.community_backend.dto.ColdArchiveData;
import petcare.example.community_backend.dto.HBaseArchiveRecord;
import petcare.example.community_backend.dto.CommunityColdStorageEvent;
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

/**
 * 社区模块冷数据迁移事件消费者服务（扁平化单表设计）
 *
 * 功能：
 * 1. RESTORE_FROM_COLD - 从冷库恢复（一站式读取）
 * 2. DELETE_FROM_COLD - 从冷库删除
 *
 * 设计原则（扁平化）：
 * 1. 恢复时只需一次 HBase Get 操作获取完整数据
 * 2. 评论和点赞从 full_data 中直接解析，无需额外查询
 * 3. 冷数据 Read-Only：归档期间不允许评论/点赞，必须先激活
 *
 * 激活流程：
 * 用户点击"激活" → 发送 RESTORE_FROM_COLD 事件 → 恢复完整数据到 MySQL → 用户可继续互动
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityColdStorageEventConsumer {

    private final PetMomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final CommunityHBaseColdStorageService hBaseService;
    private final EntityManager entityManager;

    /**
     * 处理冷存储事件
     */
    @RabbitListener(queues = RabbitMQConfig.COLD_MIGRATION_QUEUE)
    @Transactional
    public void handleColdStorageEvent(CommunityColdStorageEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ消费】收到冷存储事件: eventId={}, operationType={}, momentId={}, userId={}",
                event.getEventId(), event.getOperationType(), event.getMomentId(), event.getUserId());

        try {
            switch (event.getOperationType()) {
                case RESTORE_FROM_COLD -> handleRestoreFromCold(event);
                case DELETE_FROM_COLD -> handleDeleteFromCold(event);
                default -> log.warn("【MQ消费】未知的操作类型: eventId={}, operationType={}",
                        event.getEventId(), event.getOperationType());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ消费】冷存储事件处理完成: eventId={}, operationType={}, momentId={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), event.getMomentId(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ消费】冷存储事件处理失败: eventId={}, operationType={}, momentId={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), event.getMomentId(), elapsed, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 处理从冷库恢复（扁平化版本）
     *
     * 流程（简化版）：
     * 1. 检查 userId（必需）
     * 2. 检查 MySQL 中是否已恢复
     * 3. 从 HBase 一站式读取完整数据（包含评论和点赞）
     * 4. 批量写入 MySQL（使用 clear() 避免 Session 状态损坏）
     * 5. 事务提交后删除 HBase 数据（使用 TransactionSynchronization）
     *
     * 幂等性：
     * - 检查评论和点赞是否已存在，避免重复插入
     * - 使用 try-catch 包装单个记录，避免一个失败影响全部
     */
    private void handleRestoreFromCold(CommunityColdStorageEvent event) {
        Long momentId = event.getMomentId();
        Long userId = event.getUserId();
        log.info("【MQ消费】处理恢复请求: eventId={}, momentId={}, userId={}",
                event.getEventId(), momentId, userId);

        // 1. 检查 userId 是否存在
        if (userId == null) {
            log.error("【MQ消费】恢复事件中缺少 userId，无法恢复: eventId={}, momentId={}",
                    event.getEventId(), momentId);
            return;
        }

        // 2. 检查 MySQL 中是否已恢复
        if (momentRepository.existsById(momentId)) {
            log.info("【MQ消费】动态已在 MySQL 中，无需恢复: momentId={}", momentId);
            return;
        }

        // 3. 从 HBase 一站式读取完整归档数据
        var archiveRecordOpt = hBaseService.getArchive(userId, momentId);
        if (archiveRecordOpt.isEmpty()) {
            log.warn("【MQ消费】HBase中无此归档数据: momentId={}, userId={}", momentId, userId);
            return;
        }

        var archiveRecord = archiveRecordOpt.get();
        ColdArchiveData archiveData = hBaseService.getArchiveData(userId, momentId);

        log.info("【MQ消费】HBase读取成功: momentId={}, 评论数={}, 点赞数={}",
                momentId,
                archiveData != null && archiveData.getComments() != null ? archiveData.getComments().size() : 0,
                archiveData != null && archiveData.getLikes() != null ? archiveData.getLikes().size() : 0);

        // 4. 写入 MySQL - 动态（必须先存在，才能插入评论）
        // 使用原生SQL以保留原始ID，因为IDENTITY策略会自动生成新ID
        PetMoment newMoment = archiveRecord.toPetMoment();
        Long originalMomentId = momentId;  // 保存原始ID
        entityManager.createNativeQuery(
                "INSERT INTO moments (moment_id, user_id, content, created_at, migration_status, last_access_time) " +
                "VALUES (?, ?, ?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE moment_id = moment_id"
        ).setParameter(1, originalMomentId)
         .setParameter(2, newMoment.getUserId())
         .setParameter(3, newMoment.getContent())
         .setParameter(4, newMoment.getCreatedAt())
         .setParameter(5, "NONE")
         .setParameter(6, LocalDateTime.now())
         .executeUpdate();
        log.debug("【MQ消费】动态已写入MySQL: momentId={}", originalMomentId);

        // 5. 写入 MySQL - 评论（批量，幂等处理）
        List<ColdArchiveData.ArchivedComment> archivedComments =
                archiveData != null && archiveData.getComments() != null
                        ? archiveData.getComments()
                        : new ArrayList<>();

        int savedComments = 0;
        for (ColdArchiveData.ArchivedComment ac : archivedComments) {
            try {
                Long commentId = ac.getCommentId();
                // 幂等检查：评论是否已存在
                if (commentRepository.existsById(commentId)) {
                    log.debug("【MQ消费】评论已存在，跳过: commentId={}", commentId);
                    continue;
                }

                // 使用原生SQL以保留原始ID
                entityManager.createNativeQuery(
                        "INSERT INTO comments (comment_id, moment_id, user_id, content, parent_id, created_at, migration_status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE comment_id = comment_id"
                ).setParameter(1, commentId)
                 .setParameter(2, originalMomentId)  // 使用原始momentId
                 .setParameter(3, ac.getUserId())
                 .setParameter(4, ac.getContent())
                 .setParameter(5, ac.getParentId())
                 .setParameter(6, ac.getCreatedAt())
                 .setParameter(7, "NONE")
                 .executeUpdate();
                savedComments++;
            } catch (Exception e) {
                log.warn("【MQ消费】保存评论失败，跳过: commentId={}, error={}", ac.getCommentId(), e.getMessage());
            }
        }
        log.debug("【MQ消费】评论已写入MySQL: momentId={}, count={}", momentId, savedComments);

        // 6. 写入 MySQL - 点赞（批量，幂等处理）
        List<ColdArchiveData.ArchivedLike> archivedLikes =
                archiveData != null && archiveData.getLikes() != null
                        ? archiveData.getLikes()
                        : new ArrayList<>();

        int savedLikes = 0;
        for (ColdArchiveData.ArchivedLike al : archivedLikes) {
            try {
                Long likeId = al.getLikeId();
                // 幂等检查：点赞是否已存在
                if (likeRepository.existsById(likeId)) {
                    log.debug("【MQ消费】点赞已存在，跳过: likeId={}", likeId);
                    continue;
                }

                // 对于 MOMENT 类型的点赞，使用原始的 momentId
                Long targetId = al.getTargetType() == ColdArchiveData.ArchivedLikeTargetType.MOMENT
                        ? originalMomentId
                        : al.getTargetId();

                // 使用原生SQL以保留原始ID
                entityManager.createNativeQuery(
                        "INSERT INTO likes (like_id, user_id, target_type, target_id, created_at, migration_status) " +
                        "VALUES (?, ?, ?, ?, ?, ?) " +
                        "ON DUPLICATE KEY UPDATE like_id = like_id"
                ).setParameter(1, likeId)
                 .setParameter(2, al.getUserId())
                 .setParameter(3, al.getTargetType().name())
                 .setParameter(4, targetId)
                 .setParameter(5, al.getCreatedAt())
                 .setParameter(6, "NONE")
                 .executeUpdate();
                savedLikes++;
            } catch (Exception e) {
                log.warn("【MQ消费】保存点赞失败，跳过: likeId={}, error={}", al.getLikeId(), e.getMessage());
            }
        }
        log.debug("【MQ消费】点赞已写入MySQL: momentId={}, count={}", momentId, savedLikes);

        // 7. 事务提交后删除 HBase 归档记录（使用 TransactionSynchronization）
        //    这确保只有当 MySQL 全部写入成功后才删除冷数据
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    hBaseService.deleteArchive(userId, momentId);
                    log.info("【MQ消费】HBase归档记录已删除（事务提交后）: rowKey={}_{}", userId, momentId);
                } catch (Exception e) {
                    log.error("【MQ消费】HBase删除失败（不影响主流程）: rowKey={}_{}, error={}",
                            userId, momentId, e.getMessage());
                }
            }
        });

        log.info("【MQ消费】恢复完成: momentId={}, 评论数={}, 点赞数={}",
                momentId, savedComments, savedLikes);
    }

    /**
     * 处理从冷库删除（扁平化版本）
     *
     * 流程（简化版）：
     * 1. 检查 userId（必需，用于生成 RowKey）
     * 2. 直接从 HBase 删除整条归档记录
     *
     * 幂等性：删除不存在的记录无副作用
     */
    private void handleDeleteFromCold(CommunityColdStorageEvent event) {
        Long momentId = event.getMomentId();
        Long userId = event.getUserId();
        log.info("【MQ消费】处理删除冷库数据: eventId={}, momentId={}, userId={}",
                event.getEventId(), momentId, userId);

        // 1. 检查 userId 是否存在
        if (userId == null) {
            log.error("【MQ消费】删除事件中缺少 userId，无法删除: eventId={}, momentId={}",
                    event.getEventId(), momentId);
            return;
        }

        // 2. 直接删除归档记录（幂等操作）
        hBaseService.deleteArchive(userId, momentId);

        log.info("【MQ消费】删除冷库数据完成: momentId={}, userId={}", momentId, userId);
    }
}
