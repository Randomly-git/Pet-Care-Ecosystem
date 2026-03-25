package petcare.example.community_backend.service;

import petcare.example.community_backend.config.RabbitMQConfig;
import petcare.example.community_backend.dto.CommunityColdStorageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 社区模块冷数据迁移事件发布服务
 * 负责将迁移任务发送到消息队列
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CommunityColdStorageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布迁移到冷库事件
     */
    public void publishMigrateToColdEvent(Long momentId, Long userId,
                                         List<Long> commentIds, List<Long> likeIds) {
        CommunityColdStorageEvent event = CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(CommunityColdStorageEvent.ColdStorageOperationType.MIGRATE_TO_COLD)
                .targetType(CommunityColdStorageEvent.TargetType.MOMENT)
                .momentId(momentId)
                .userId(userId)
                .relatedCommentIds(commentIds)
                .relatedLikeIds(likeIds)
                .createdAt(java.time.LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发布从冷库恢复事件
     *
     * @param momentId 动态ID
     * @param userId   用户ID（用于拼装 RowKey）
     */
    public void publishRestoreFromColdEvent(Long momentId, Long userId) {
        CommunityColdStorageEvent event = CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(CommunityColdStorageEvent.ColdStorageOperationType.RESTORE_FROM_COLD)
                .targetType(CommunityColdStorageEvent.TargetType.MOMENT)
                .momentId(momentId)
                .userId(userId)
                .createdAt(java.time.LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发布从冷库删除事件
     *
     * @param momentId 动态ID
     * @param userId   用户ID（用于拼装 HBase RowKey）
     */
    public void publishDeleteFromColdEvent(Long momentId, Long userId) {
        if (userId == null) {
            log.error("【MQ发布】删除冷库事件缺少 userId，无法删除: momentId={}", momentId);
            throw new IllegalArgumentException("删除冷库数据必须提供 userId");
        }

        CommunityColdStorageEvent event = CommunityColdStorageEvent.builder()
                .eventId(java.util.UUID.randomUUID().toString())
                .operationType(CommunityColdStorageEvent.ColdStorageOperationType.DELETE_FROM_COLD)
                .targetType(CommunityColdStorageEvent.TargetType.MOMENT)
                .momentId(momentId)
                .userId(userId)
                .createdAt(java.time.LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发送事件到消息队列
     */
    private void publishEvent(CommunityColdStorageEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ发布】开始发送冷存储事件: eventId={}, operationType={}, momentId={}",
                event.getEventId(), event.getOperationType(), event.getMomentId());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COLD_MIGRATION_EXCHANGE,
                    RabbitMQConfig.COLD_MIGRATION_ROUTING_KEY,
                    event
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ发布】冷存储事件发送成功: eventId={}, operationType={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ发布】冷存储事件发送失败: eventId={}, operationType={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), elapsed, e.getMessage(), e);
            throw e;
        }
    }
}
