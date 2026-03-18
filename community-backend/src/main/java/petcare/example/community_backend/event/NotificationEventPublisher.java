package petcare.example.community_backend.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import static petcare.example.community_backend.config.RabbitMQConfig.*;

/**
 * 通知事件发布者
 * 负责将通知事件发布到 RabbitMQ
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    /**
     * 发送点赞通知
     */
    public void publishLikeNotification(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    NOTIFICATION_EXCHANGE,
                    NOTIFICATION_LIKE_ROUTING_KEY,
                    event
            );
            log.info("【MQ】点赞通知已发送: eventId={}, targetUserId={}, businessId={}",
                    event.getEventId(), event.getTargetUserId(), event.getBusinessId());
        } catch (Exception e) {
            log.error("【MQ】发送点赞通知失败: eventId={}, error={}",
                    event.getEventId(), e.getMessage());
        }
    }

    /**
     * 发送评论通知
     */
    public void publishCommentNotification(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    NOTIFICATION_EXCHANGE,
                    NOTIFICATION_COMMENT_ROUTING_KEY,
                    event
            );
            log.info("【MQ】评论通知已发送: eventId={}, targetUserId={}, businessId={}",
                    event.getEventId(), event.getTargetUserId(), event.getBusinessId());
        } catch (Exception e) {
            log.error("【MQ】发送评论通知失败: eventId={}, error={}",
                    event.getEventId(), e.getMessage());
        }
    }

    /**
     * 发送关注通知
     */
    public void publishFollowNotification(NotificationEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    NOTIFICATION_EXCHANGE,
                    NOTIFICATION_FOLLOW_ROUTING_KEY,
                    event
            );
            log.info("【MQ】关注通知已发送: eventId={}, targetUserId={}",
                    event.getEventId(), event.getTargetUserId());
        } catch (Exception e) {
            log.error("【MQ】发送关注通知失败: eventId={}, error={}",
                    event.getEventId(), e.getMessage());
        }
    }
}
