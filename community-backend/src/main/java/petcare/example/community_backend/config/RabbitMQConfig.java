// config/RabbitMQConfig.java
package petcare.example.community_backend.config;

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

    // ==================== 队列 ====================
    public static final String MEDIA_BIND_QUEUE = "petcare.media.bind.queue";
    public static final String MEDIA_BIND_DLQ = "petcare.media.bind.dlq";

    // ==================== 路由键 ====================
    public static final String MEDIA_BIND_ROUTING_KEY = "media.bind";
    public static final String MEDIA_BIND_DL_ROUTING_KEY = "media.bind.dl";

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

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
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
