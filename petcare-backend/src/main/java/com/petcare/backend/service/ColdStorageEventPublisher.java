package com.petcare.backend.service;

import com.petcare.backend.config.ActivityColdDataRabbitMQConfig;
import com.petcare.backend.dto.ColdStorageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 冷数据迁移事件发布服务
 * 负责将迁移任务发送到消息队列
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ColdStorageEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布迁移到冷库事件
     */
    public void publishMigrateToColdEvent(Long activityRecordId, Long petId, Long activityId, 
                                          LocalDateTime activityDate) {
        ColdStorageEvent event = ColdStorageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(ColdStorageEvent.ColdStorageOperationType.MIGRATE_TO_COLD)
                .activityRecordId(activityRecordId)
                .petId(petId)
                .activityId(activityId)
                .activityDate(activityDate)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发布解冻事件
     */
    public void publishThawFromColdEvent(Long activityRecordId, Long petId, Long activityId,
                                        LocalDateTime activityDate) {
        ColdStorageEvent event = ColdStorageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(ColdStorageEvent.ColdStorageOperationType.THAW_FROM_COLD)
                .activityRecordId(activityRecordId)
                .petId(petId)
                .activityId(activityId)
                .activityDate(activityDate)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发布删除冷库数据事件
     */
    public void publishDeleteFromColdEvent(Long activityRecordId, Long petId, Long activityId,
                                           LocalDateTime activityDate) {
        ColdStorageEvent event = ColdStorageEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(ColdStorageEvent.ColdStorageOperationType.DELETE_FROM_COLD)
                .activityRecordId(activityRecordId)
                .petId(petId)
                .activityId(activityId)
                .activityDate(activityDate)
                .createdAt(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发送事件到消息队列
     */
    private void publishEvent(ColdStorageEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ发布】开始发送冷存储事件: eventId={}, operationType={}, activityRecordId={}",
                event.getEventId(), event.getOperationType(), event.getActivityRecordId());

        try {
            rabbitTemplate.convertAndSend(
                    ActivityColdDataRabbitMQConfig.COLD_MIGRATION_EXCHANGE,
                    ActivityColdDataRabbitMQConfig.COLD_MIGRATION_ROUTING_KEY,
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
