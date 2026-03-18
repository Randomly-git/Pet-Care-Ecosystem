// event/NotificationEventConsumer.java
package petcare.example.community_backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import petcare.example.community_backend.entity.Notification;
import petcare.example.community_backend.repository.NotificationRepository;
import petcare.example.community_backend.service.WebSocketService;

import java.time.LocalDateTime;

import static petcare.example.community_backend.config.RabbitMQConfig.NOTIFICATION_QUEUE;

/**
 * 通知事件消费者
 * 负责处理来自 MQ 的通知事件，保存通知并推送 WebSocket
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;

    /**
     * 处理通知事件
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE)
    public void handleNotificationEvent(NotificationEvent event) {
        long startTime = System.currentTimeMillis();

        try {
            // 记录接收到的消息
            log.info("【MQ消费】收到通知事件: eventId={}, type={}, actorUserId={}, targetUserId={}, content={}",
                    event.getEventId(),
                    event.getType(),
                    event.getActorUserId(),
                    event.getTargetUserId(),
                    event.getContent());

            // 计算消息延迟
            long messageDelay = System.currentTimeMillis() - event.getEventTime().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            log.info("【MQ监控】通知消息延迟: eventId={}, 延迟={}ms", event.getEventId(), messageDelay);

            // 验证事件数据
            if (event.getTargetUserId() == null || event.getActorUserId() == null) {
                log.warn("【MQ消费】通知事件数据不完整，跳过处理: eventId={}", event.getEventId());
                return;
            }

            // 避免自己给自己发通知
            if (event.getTargetUserId().equals(event.getActorUserId())) {
                log.debug("【MQ消费】跳过自我通知: eventId={}", event.getEventId());
                return;
            }

            // 1. 保存通知到数据库
            Notification notification = saveNotification(event);
            log.info("【MQ消费】通知已保存: id={}, userId={}", notification.getId(), notification.getUserId());

            // 2. 通过 WebSocket 推送实时通知
            webSocketService.sendNotificationToUser(event.getTargetUserId(), notification);
            log.info("【MQ消费】WebSocket 通知已推送: userId={}", event.getTargetUserId());

            // 处理完成
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("【MQ消费】通知事件处理完成: eventId={}, 耗时={}ms", event.getEventId(), elapsed);

            // 性能监控报告
            printPerformanceReport(event, elapsed);

        } catch (Exception e) {
            log.error("【MQ消费】通知事件处理失败: eventId={}, 耗时={}ms, error={}",
                    event.getEventId(),
                    System.currentTimeMillis() - startTime,
                    e.getMessage(), e);
            throw e; // 重新抛出异常，让 MQ 进行重试
        }
    }

    /**
     * 保存通知到数据库
     */
    private Notification saveNotification(NotificationEvent event) {
        Notification notification = Notification.builder()
                .type(event.getType() != null ? event.getType().name() : "UNKNOWN")
                .userId(event.getTargetUserId())
                .actorUserId(event.getActorUserId())
                .actorUserName(event.getActorUserName())
                .actorUserAvatar(null) // 可以后续通过用户服务获取
                .businessId(event.getBusinessId())
                .businessType(event.getBusinessType())
                .content(event.getContent())
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        return notificationRepository.save(notification);
    }

    /**
     * 打印性能报告
     */
    private void printPerformanceReport(NotificationEvent event, long elapsed) {
        log.info("========== 【通知 MQ 性能监控报告】 ==========");
        log.info("事件类型: {}", event.getType());
        log.info("事件ID: {}", event.getEventId());
        log.info("触发用户: {} ({})", event.getActorUserName(), event.getActorUserId());
        log.info("目标用户: {}", event.getTargetUserId());
        log.info("业务ID: {}", event.getBusinessId());
        log.info("处理耗时: {}ms", elapsed);

        if (elapsed < 50) {
            log.info("性能评价: 优秀 (<50ms)");
        } else if (elapsed < 100) {
            log.info("性能评价: 良好 (50-100ms)");
        } else if (elapsed < 200) {
            log.info("性能评价: 一般 (100-200ms)");
        } else {
            log.info("性能评价: 需优化 (>200ms)");
        }
        log.info("============================================");
    }
}
