// config/RabbitMQConfig.java
package petcare.example.community_backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // ==================== 交换机 ====================
    public static final String MEDIA_EXCHANGE = "petcare.media.exchange";
    public static final String NOTIFICATION_EXCHANGE = "petcare.notification.exchange";

    // ==================== 队列 ====================
    public static final String MEDIA_BIND_QUEUE = "petcare.media.bind.queue";
    public static final String MEDIA_BIND_DLQ = "petcare.media.bind.dlq";
    public static final String NOTIFICATION_QUEUE = "petcare.notification.queue";
    public static final String NOTIFICATION_DLQ = "petcare.notification.dlq";

    // ==================== 路由键 ====================
    public static final String MEDIA_BIND_ROUTING_KEY = "media.bind";
    public static final String MEDIA_BIND_DL_ROUTING_KEY = "media.bind.dl";
    // 通知路由键
    public static final String NOTIFICATION_LIKE_ROUTING_KEY = "notification.like";
    public static final String NOTIFICATION_COMMENT_ROUTING_KEY = "notification.comment";
    public static final String NOTIFICATION_FOLLOW_ROUTING_KEY = "notification.follow";

    // ==================== 媒体相关 Bean ====================

    /**
     * 声明交换机
     */
    @Bean
    public DirectExchange mediaExchange() {
        return new DirectExchange(MEDIA_EXCHANGE, true, false);
    }

    /**
     * 声明媒体关联队列
     */
    @Bean
    public Queue mediaBindQueue() {
        return QueueBuilder.durable(MEDIA_BIND_QUEUE)
                .withArgument("x-dead-letter-exchange", MEDIA_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MEDIA_BIND_DL_ROUTING_KEY)
                .build();
    }

    /**
     * 声明死信队列
     */
    @Bean
    public Queue mediaBindDLQueue() {
        return QueueBuilder.durable(MEDIA_BIND_DLQ).build();
    }

    /**
     * 绑定队列到交换机
     */
    @Bean
    public Binding mediaBindBinding() {
        return BindingBuilder.bind(mediaBindQueue())
                .to(mediaExchange())
                .with(MEDIA_BIND_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到交换机
     */
    @Bean
    public Binding mediaBindDLBinding() {
        return BindingBuilder.bind(mediaBindDLQueue())
                .to(mediaExchange())
                .with(MEDIA_BIND_DL_ROUTING_KEY);
    }

    // ==================== 通知相关 Bean ====================

    /**
     * 通知交换机
     */
    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    /**
     * 通知队列
     */
    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", NOTIFICATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", "notification.dl")
                .build();
    }

    /**
     * 通知死信队列
     */
    @Bean
    public Queue notificationDLQueue() {
        return QueueBuilder.durable(NOTIFICATION_DLQ).build();
    }

    /**
     * 绑定通知队列 - 点赞通知
     */
    @Bean
    public Binding notificationLikeBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_LIKE_ROUTING_KEY);
    }

    /**
     * 绑定通知队列 - 评论通知
     */
    @Bean
    public Binding notificationCommentBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_COMMENT_ROUTING_KEY);
    }

    /**
     * 绑定通知队列 - 关注通知
     */
    @Bean
    public Binding notificationFollowBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NOTIFICATION_FOLLOW_ROUTING_KEY);
    }

    /**
     * 绑定通知死信队列
     */
    @Bean
    public Binding notificationDLBinding() {
        return BindingBuilder.bind(notificationDLQueue())
                .to(notificationExchange())
                .with("notification.dl");
    }

    // ==================== 通用配置 ====================

    /**
     * JSON 消息转换器 - 支持 Java 8 日期时间
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 注册 Java 8 日期时间模块
        objectMapper.registerModule(new JavaTimeModule());
        // 禁用将日期时间写为时间戳的方式
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
