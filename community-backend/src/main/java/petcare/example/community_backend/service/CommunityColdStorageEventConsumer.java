package petcare.example.community_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * 4. 批量写入 MySQL
     * 5. 从 HBase 删除
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
        PetMoment newMoment = archiveRecord.toPetMoment();
        newMoment.setId(momentId);  // 使用事件的 momentId，而不是 HBase 中的值
        momentRepository.save(newMoment);
        momentRepository.flush();  // 确保 moment 立即写入，获取 ID
        log.debug("【MQ消费】动态已写入MySQL: momentId={}", momentId);

        // 5. 写入 MySQL - 评论（批量，幂等处理）
        List<ColdArchiveData.ArchivedComment> archivedComments =
                archiveData != null && archiveData.getComments() != null
                        ? archiveData.getComments()
                        : new ArrayList<>();

        List<Comment> savedComments = new ArrayList<>();
        for (ColdArchiveData.ArchivedComment ac : archivedComments) {
            try {
                Long commentId = ac.getCommentId();
                // 幂等检查：评论是否已存在
                if (commentRepository.existsById(commentId)) {
                    log.debug("【MQ消费】评论已存在，跳过: commentId={}", commentId);
                    continue;
                }
                
                // 直接创建 Comment 实体，使用事件的 momentId
                Comment comment = new Comment();
                comment.setId(commentId);
                comment.setMomentId(momentId);  // 使用事件的 momentId
                comment.setUserId(ac.getUserId());
                comment.setContent(ac.getContent());
                comment.setParentId(ac.getParentId());
                comment.setCreatedAt(ac.getCreatedAt());
                comment.setMigrationStatus("NONE");
                
                commentRepository.save(comment);
                savedComments.add(comment);
            } catch (Exception e) {
                log.warn("【MQ消费】保存评论失败，跳过: commentId={}, error={}", ac.getCommentId(), e.getMessage());
            }
        }
        log.debug("【MQ消费】评论已写入MySQL: momentId={}, count={}", momentId, savedComments.size());

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
                
                // 直接创建 Like 实体，使用事件的 momentId
                Like like = new Like();
                like.setId(likeId);
                like.setUserId(al.getUserId());
                like.setTargetType(TargetType.valueOf(al.getTargetType().name()));
                // 对于 MOMENT 类型的点赞，使用事件的 momentId
                if (al.getTargetType() == ColdArchiveData.ArchivedLikeTargetType.MOMENT) {
                    like.setTargetId(momentId);
                } else {
                    like.setTargetId(al.getTargetId());  // 评论点赞保持原值
                }
                like.setCreatedAt(al.getCreatedAt());
                like.setMigrationStatus("NONE");
                
                likeRepository.save(like);
                savedLikes++;
            } catch (Exception e) {
                log.warn("【MQ消费】保存点赞失败，跳过: likeId={}, error={}", al.getLikeId(), e.getMessage());
            }
        }
        log.debug("【MQ消费】点赞已写入MySQL: momentId={}, count={}", momentId, savedLikes);

        // 7. 从 HBase 删除归档记录
        hBaseService.deleteArchive(userId, momentId);

        log.info("【MQ消费】恢复完成: momentId={}, 评论数={}, 点赞数={}",
                momentId, savedComments.size(), savedLikes);
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
