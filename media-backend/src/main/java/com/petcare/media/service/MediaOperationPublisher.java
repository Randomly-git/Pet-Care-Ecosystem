// service/MediaOperationPublisher.java
package com.petcare.media.service;

import com.petcare.media.config.RabbitMQConfig;
import com.petcare.media.dto.MediaOperationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 媒体操作事件发布服务
 * 负责将 COS 操作事件发送到消息队列（异步处理）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaOperationPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布设置存储类型事件（转冷/转热）
     *
     * @param mediaId          媒体文件ID
     * @param fileUrl          媒体文件URL
     * @param targetStorageClass 目标存储类型 (ARCHIVE / STANDARD)
     */
    public void publishSetStorageClassEvent(Long mediaId, String fileUrl, String targetStorageClass) {
        MediaOperationEvent event = MediaOperationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(MediaOperationEvent.MediaOperationType.SET_STORAGE_CLASS)
                .mediaId(mediaId)
                .fileUrl(fileUrl)
                .targetStorageClass(targetStorageClass)
                .tagKey("Status")
                .tagValue("Cold")
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .description("设置存储类型: " + targetStorageClass)
                .build();

        publishEvent(event);
    }

    /**
     * 发布恢复归档文件事件
     *
     * @param mediaId  媒体文件ID
     * @param fileUrl  媒体文件URL
     */
    public void publishRestoreArchivedEvent(Long mediaId, String fileUrl) {
        MediaOperationEvent event = MediaOperationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(MediaOperationEvent.MediaOperationType.RESTORE_ARCHIVED)
                .mediaId(mediaId)
                .fileUrl(fileUrl)
                .targetStorageClass("STANDARD")
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .description("恢复归档文件")
                .build();

        publishEvent(event);
    }

    /**
     * 发布删除文件事件
     *
     * @param mediaId  媒体文件ID
     * @param fileUrl  媒体文件URL
     */
    public void publishDeleteFileEvent(Long mediaId, String fileUrl) {
        MediaOperationEvent event = MediaOperationEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .operationType(MediaOperationEvent.MediaOperationType.DELETE_FILE)
                .mediaId(mediaId)
                .fileUrl(fileUrl)
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .description("删除文件")
                .build();

        publishEvent(event);
    }

    /**
     * 发送事件到消息队列
     */
    private void publishEvent(MediaOperationEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ发布】开始发送媒体操作事件: eventId={}, operationType={}, mediaId={}, fileUrl={}",
                event.getEventId(), event.getOperationType(), event.getMediaId(), event.getFileUrl());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.COLD_OPERATION_EXCHANGE,
                    RabbitMQConfig.COLD_OPERATION_ROUTING_KEY,
                    event
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ发布】媒体操作事件发送成功: eventId={}, operationType={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ发布】媒体操作事件发送失败: eventId={}, operationType={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), elapsed, e.getMessage(), e);
            throw e;
        }
    }
}
