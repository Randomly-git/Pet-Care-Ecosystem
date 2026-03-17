// service/MediaEventPublisher.java
package petcare.example.community_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import petcare.example.community_backend.config.RabbitMQConfig;
import petcare.example.community_backend.dto.MediaBindEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 媒体事件发布服务
 * 负责将媒体关联事件发送到消息队列
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发布社区动态媒体关联事件
     *
     * @param momentId  动态ID
     * @param mediaIds  媒体文件ID列表
     * @param userId   用户ID
     */
    public void publishCommunityMediaBindEvent(Long momentId, List<Long> mediaIds, Long userId) {
        MediaBindEvent event = MediaBindEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventType("COMMUNITY_MOMENT")
                .businessId(momentId)
                .mediaIds(mediaIds)
                .relatedType("MOMENT")
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .retryCount(0)
                .maxRetries(3)
                .build();

        publishEvent(event);
    }

    /**
     * 发送事件到消息队列
     */
    private void publishEvent(MediaBindEvent event) {
        long startTime = System.currentTimeMillis();
        log.info("【MQ发布】开始发送媒体关联事件: eventId={}, eventType={}, businessId={}, mediaIds={}",
                event.getEventId(), event.getEventType(), event.getBusinessId(), event.getMediaIds());

        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.MEDIA_EXCHANGE,
                    RabbitMQConfig.MEDIA_BIND_ROUTING_KEY,
                    event
            );

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ发布】媒体关联事件发送成功: eventId={}, 耗时={}ms",
                    event.getEventId(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("【MQ发布】媒体关联事件发送失败: eventId={}, 耗时={}ms, error={}",
                    event.getEventId(), elapsed, e.getMessage(), e);
            throw e;
        }
    }
}
