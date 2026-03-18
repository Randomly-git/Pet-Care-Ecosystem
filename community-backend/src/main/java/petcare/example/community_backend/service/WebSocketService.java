// service/WebSocketService.java
package petcare.example.community_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 服务
 * 负责实时推送通知给用户
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 发送通知给指定用户
     * 推送到 /queue/user/{userId} 队列
     */
    public void sendNotificationToUser(Long userId, Object notification) {
        try {
            String destination = "/queue/user/" + userId;
            messagingTemplate.convertAndSend(destination, notification);
            log.info("【WebSocket推送】通知已发送给用户: userId={}, destination={}", userId, destination);
        } catch (Exception e) {
            log.error("【WebSocket推送】发送通知失败: userId={}, error={}", userId, e.getMessage(), e);
        }
    }

    /**
     * 广播通知给所有用户
     * 推送到 /topic/notifications 队列
     */
    public void broadcastNotification(Object notification) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", notification);
            log.info("【WebSocket推送】广播通知已发送");
        } catch (Exception e) {
            log.error("【WebSocket推送】广播通知失败: error={}", e.getMessage(), e);
        }
    }

    /**
     * 发送消息给指定用户（通用方法）
     */
    public void sendMessage(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            log.debug("【WebSocket推送】消息已发送: destination={}", destination);
        } catch (Exception e) {
            log.error("【WebSocket推送】发送消息失败: destination={}, error={}", destination, e.getMessage(), e);
        }
    }
}
