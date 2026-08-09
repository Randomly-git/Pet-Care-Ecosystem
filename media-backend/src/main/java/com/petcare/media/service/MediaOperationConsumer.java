// service/MediaOperationConsumer.java
package com.petcare.media.service;

import com.petcare.media.config.RabbitMQConfig;
import com.petcare.media.dto.MediaOperationEvent;
import com.petcare.media.entity.MediaFile;
import com.petcare.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 媒体操作事件消费者服务
 * 接收并处理 COS 操作事件（设置标签、恢复归档等）
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaOperationConsumer {

    private final MediaRepository mediaRepository;
    private final CosStorageService cosStorageService;

    /**
     * 处理媒体操作事件
     * 监听 petcare.cold.operation.queue 队列
     */
    @RabbitListener(queues = RabbitMQConfig.COLD_OPERATION_QUEUE)
    @Transactional
    public void handleMediaOperationEvent(MediaOperationEvent event) {
        // RabbitListener 将队列消息反序列化为 MediaOperationEvent，并在独立消费者线程执行。
        long startTime = System.currentTimeMillis();
        log.info("【MQ消费】收到媒体操作事件: eventId={}, operationType={}, mediaId={}, fileUrl={}",
                event.getEventId(), event.getOperationType(), event.getMediaId(), event.getFileUrl());

        try {
            // 一个事件只处理一个文件，失败可以独立重试，不影响其他文件。
            switch (event.getOperationType()) {
                case SET_STORAGE_CLASS -> handleSetStorageClass(event);
                case RESTORE_ARCHIVED -> handleRestoreArchived(event);
                case DELETE_FILE -> handleDeleteFile(event);
                default -> log.warn("【MQ消费】未知的操作类型: eventId={}, operationType={}",
                        event.getEventId(), event.getOperationType());
            }

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ消费】媒体操作处理完成: eventId={}, operationType={}, mediaId={}, 耗时={}ms",
                    event.getEventId(), event.getOperationType(), event.getMediaId(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ消费】媒体操作处理失败: eventId={}, operationType={}, mediaId={}, 耗时={}ms, error={}",
                    event.getEventId(), event.getOperationType(), event.getMediaId(), elapsed, e.getMessage(), e);
            // 必须继续抛出异常；吞掉异常会让 Spring 误以为消费成功，消息无法重试或进入死信队列。
            throw e;
        }
    }

    /**
     * 处理设置存储类型事件（转冷/转热）
     */
    private void handleSetStorageClass(MediaOperationEvent event) {
        log.info("【MQ消费】开始设置存储类型: eventId={}, mediaId={}, targetStorageClass={}",
                event.getEventId(), event.getMediaId(), event.getTargetStorageClass());

        // 第一步修改 COS 存储标签，ARCHIVE 表示冷存储，其他目标表示恢复热存储。
        cosStorageService.setFileTagging(
                event.getFileUrl(),
                event.getTagKey(),
                event.getTagValue()
        );

        // 第二步把 COS 的最终结果同步回 MySQL，状态机由 Archiving/Restoring 转为 Cold/Hot。
        MediaFile mediaFile = mediaRepository.findById(event.getMediaId()).orElse(null);
        if (mediaFile != null) {
            // 根据目标存储类型设置状态
            String status = "ARCHIVE".equalsIgnoreCase(event.getTargetStorageClass()) ? "Cold" : "Hot";
            mediaFile.setStatus(status);
            mediaRepository.save(mediaFile);

            log.info("【MQ消费】存储类型设置成功并更新数据库: eventId={}, mediaId={}, status={}",
                    event.getEventId(), event.getMediaId(), status);
        } else {
            log.warn("【MQ消费】媒体文件不存在: eventId={}, mediaId={}",
                    event.getEventId(), event.getMediaId());
        }
    }

    /**
     * 处理恢复归档文件事件
     */
    private void handleRestoreArchived(MediaOperationEvent event) {
        log.info("【MQ消费】开始恢复归档文件: eventId={}, mediaId={}",
                event.getEventId(), event.getMediaId());

        // 1. 调用 COS API 恢复归档文件
        cosStorageService.restoreArchivedFile(event.getFileUrl());

        // 2. 更新数据库状态
        MediaFile mediaFile = mediaRepository.findById(event.getMediaId()).orElse(null);
        if (mediaFile != null) {
            mediaFile.setStatus("Hot");
            mediaRepository.save(mediaFile);

            log.info("【MQ消费】归档文件恢复成功并更新数据库: eventId={}, mediaId={}, status=Hot",
                    event.getEventId(), event.getMediaId());
        } else {
            log.warn("【MQ消费】媒体文件不存在: eventId={}, mediaId={}",
                    event.getEventId(), event.getMediaId());
        }
    }

    /**
     * 处理删除文件事件
     * 注意：数据库记录已在 MediaService 中提前删除，此处仅处理 COS 文件删除
     */
    private void handleDeleteFile(MediaOperationEvent event) {
        log.info("【MQ消费】收到COS文件删除任务: eventId={}, mediaId={}, fileUrl={}",
                event.getEventId(), event.getMediaId(), event.getFileUrl());

        try {
            // 数据库记录已由 MediaService 提前删除；这里异步删除真实 COS 对象。
            cosStorageService.deleteFile(event.getFileUrl());

            log.info("【MQ消费】COS文件删除成功: eventId={}, mediaId={}, fileUrl={}",
                    event.getEventId(), event.getMediaId(), event.getFileUrl());

        } catch (Exception e) {
            log.error("【MQ消费】COS文件删除失败: eventId={}, mediaId={}, fileUrl={}, error={}",
                    event.getEventId(), event.getMediaId(), event.getFileUrl(), e.getMessage(), e);
            // 抛出异常触发重试机制
            throw e;
        }
    }
}
