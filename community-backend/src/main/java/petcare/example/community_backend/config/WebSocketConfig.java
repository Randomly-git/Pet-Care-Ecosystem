// config/WebSocketConfig.java
package petcare.example.community_backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置
 * 启用 STOMP 协议支持实时消息推送
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单内存消息代理，用于广播
        config.enableSimpleBroker("/topic", "/queue");
        // 配置应用程序目标前缀（客户端发送消息到此前缀）
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 连接端点（支持 SockJS）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // 纯 WebSocket 连接端点（不支持 SockJS）
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
    }
}
