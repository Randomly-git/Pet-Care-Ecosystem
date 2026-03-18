package petcare.example.community_backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import static petcare.example.community_backend.config.RabbitMQConfig.NOTIFICATION_QUEUE;

/**
 * 通知事件消费者
 * 负责处理来自 MQ 的通知事件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    // 后续可以注入通知服务、WebSocket服务等进行实际处理
    // private final NotificationService notificationService;
    // private final WebSocketService webSocketService;

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

            // TODO: 这里可以调用通知服务保存通知记录
            // 例如：notificationService.saveNotification(event);

            // TODO: 可以通过 WebSocket 实时推送通知
            // 例如：webSocketService.sendNotification(event.getTargetUserId(), event);

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
