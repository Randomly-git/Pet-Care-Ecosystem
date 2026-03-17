// service/MediaEventConsumer.java
package com.petcare.media.service;

import com.petcare.media.config.RabbitMQConfig;
import com.petcare.media.dto.MediaBindEvent;
import com.petcare.media.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 媒体事件消费者服务
 * 接收并处理媒体关联事件，同时记录耗时
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventConsumer {

    private final MediaRepository mediaRepository;

    /**
     * 处理媒体关联事件
     * 监听 petcare.media.bind.queue 队列
     */
    @RabbitListener(queues = RabbitMQConfig.MEDIA_BIND_QUEUE)
    @Transactional
    public void handleMediaBindEvent(MediaBindEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ消费】收到媒体关联事件: eventId={}, eventType={}, businessId={}, mediaIds={}",
                event.getEventId(), event.getEventType(), event.getBusinessId(), event.getMediaIds());

        // 计算消息从发送到接收的延迟
        if (event.getTimestamp() != null) {
            long messageDelay = Duration.between(event.getTimestamp(), LocalDateTime.now()).toMillis();
            log.info("【MQ监控】消息队列延迟: eventId={}, 延迟={}ms", event.getEventId(), messageDelay);
        }

        try {
            // 处理媒体关联（更新关联关系）
            if (event.getMediaIds() != null && !event.getMediaIds().isEmpty()
                    && event.getBusinessId() != null && event.getRelatedType() != null) {

                // 执行关联更新
                int updatedCount = mediaRepository.batchUpdateRelatedId(
                        event.getMediaIds(),
                        event.getRelatedType(),
                        event.getBusinessId()
                );

                long endTime = System.currentTimeMillis();
                long elapsed = endTime - startTime;

                log.info("【MQ消费】媒体关联处理完成: eventId={}, 更新数量={}, 耗时={}ms",
                        event.getEventId(), updatedCount, elapsed);

                // 输出性能监控日志
                logPerformanceMetrics(event, elapsed, updatedCount);

            } else {
                log.warn("【MQ消费】事件数据不完整，跳过处理: eventId={}", event.getEventId());
            }

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long elapsed = endTime - startTime;

            log.error("【MQ消费】媒体关联处理失败: eventId={}, 耗时={}ms, error={}",
                    event.getEventId(), elapsed, e.getMessage(), e);

            // 这里可以根据重试次数决定是否重新入队
            handleRetry(event, e);
        }
    }

    /**
     * 输出性能指标日志
     */
    private void logPerformanceMetrics(MediaBindEvent event, long elapsed, int updatedCount) {
        log.info("========== 【MQ性能监控报告】 ==========");
        log.info("事件类型: {}", event.getEventType());
        log.info("事件ID: {}", event.getEventId());
        log.info("业务ID: {}", event.getBusinessId());
        log.info("媒体数量: {}", event.getMediaIds() != null ? event.getMediaIds().size() : 0);
        log.info("实际更新数量: {}", updatedCount);
        log.info("处理耗时: {}ms", elapsed);

        // 根据耗时给出性能评价
        if (elapsed < 50) {
            log.info("性能评价: 优秀 (<50ms)");
        } else if (elapsed < 100) {
            log.info("性能评价: 良好 (50-100ms)");
        } else if (elapsed < 200) {
            log.info("性能评价: 一般 (100-200ms)");
        } else {
            log.info("性能评价: 需优化 (>200ms)");
        }
        log.info("=======================================");
    }

    /**
     * 处理重试逻辑
     */
    private void handleRetry(MediaBindEvent event, Exception e) {
        // 可以在这里实现重试逻辑，例如：
        // 1. 增加重试次数
        // 2. 重新发送到队列（带延迟）
        // 3. 记录到死信队列

        log.warn("【MQ消费】准备处理重试: eventId={}, 当前重试次数={}, 最大重试次数={}",
                event.getEventId(), event.getRetryCount(), event.getMaxRetries());

        // 如果还有重试次数，可以记录日志或发送告警
        if (event.getRetryCount() >= event.getMaxRetries()) {
            log.error("【MQ消费】重试次数已达上限，将进入死信队列: eventId={}", event.getEventId());
        }
    }
}
