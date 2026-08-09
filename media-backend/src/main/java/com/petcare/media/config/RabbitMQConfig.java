// config/RabbitMQConfig.java
package com.petcare.media.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Spring 启动时声明交换机、队列和绑定；生产者与消费者只依赖这些稳定名称。

    // ==================== 媒体关联事件交换机/队列 ====================
    public static final String MEDIA_EXCHANGE = "petcare.media.exchange";

    // ==================== 队列 ====================
    public static final String MEDIA_BIND_QUEUE = "petcare.media.bind.queue";
    public static final String MEDIA_BIND_DLQ = "petcare.media.bind.dlq";

    // ==================== 路由键 ====================
    public static final String MEDIA_BIND_ROUTING_KEY = "media.bind";
    public static final String MEDIA_BIND_DL_ROUTING_KEY = "media.bind.dl";

    // ==================== 冷数据操作交换机/队列 ====================
    public static final String COLD_OPERATION_EXCHANGE = "petcare.cold.operation.exchange";
    public static final String COLD_OPERATION_QUEUE = "petcare.cold.operation.queue";
    public static final String COLD_OPERATION_DLQ = "petcare.cold.operation.dlq";

    // ==================== 路由键 ====================
    public static final String COLD_OPERATION_ROUTING_KEY = "cold.operation";
    public static final String COLD_OPERATION_DL_ROUTING_KEY = "cold.operation.dl";

    /**
     * 声明交换机 - 媒体关联
     */
    @Bean
    public DirectExchange mediaExchange() {
        // 媒体关联使用 DirectExchange，media.bind 路由键只投递到媒体绑定队列。
        return new DirectExchange(MEDIA_EXCHANGE, true, false);
    }

    /**
     * 声明媒体关联队列
     */
    @Bean
    public Queue mediaBindQueue() {
        // durable 队列保存待关联媒体；失败消息通过 DLX 路由到媒体关联死信队列。
        return QueueBuilder.durable(MEDIA_BIND_QUEUE)
                .withArgument("x-dead-letter-exchange", MEDIA_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MEDIA_BIND_DL_ROUTING_KEY)
                .build();
    }

    /**
     * 声明死信队列 - 媒体关联
     */
    @Bean
    public Queue mediaBindDLQueue() {
        // 死信队列不自动消费，供运维查看失败原因或编写补偿程序。
        return QueueBuilder.durable(MEDIA_BIND_DLQ).build();
    }

    /**
     * 绑定队列到交换机 - 媒体关联
     */
    @Bean
    public Binding mediaBindBinding() {
        // 正常媒体关联消息的交换机到队列路由关系。
        return BindingBuilder.bind(mediaBindQueue())
                .to(mediaExchange())
                .with(MEDIA_BIND_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到交换机 - 媒体关联
     */
    @Bean
    public Binding mediaBindDLBinding() {
        // 死信路由关系；没有该绑定时消息会被交换机丢弃。
        return BindingBuilder.bind(mediaBindDLQueue())
                .to(mediaExchange())
                .with(MEDIA_BIND_DL_ROUTING_KEY);
    }

    // ==================== 冷数据操作队列配置 ====================

    /**
     * 声明交换机 - 冷数据操作
     */
    @Bean
    public DirectExchange coldOperationExchange() {
        // 冷操作交换机承载 SET_STORAGE_CLASS、RESTORE_ARCHIVED、DELETE_FILE 三类事件。
        return new DirectExchange(COLD_OPERATION_EXCHANGE, true, false);
    }

    /**
     * 声明冷数据操作队列
     */
    @Bean
    public Queue coldOperationQueue() {
        // 定时归档任务只发布消息，COS 调用由消费者异步执行，从而削峰和重试。
        return QueueBuilder.durable(COLD_OPERATION_QUEUE)
                .withArgument("x-dead-letter-exchange", COLD_OPERATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", COLD_OPERATION_DL_ROUTING_KEY)
                .build();
    }

    /**
     * 声明死信队列 - 冷数据操作
     */
    @Bean
    public Queue coldOperationDLQueue() {
        // COS 调用或数据库更新持续失败的消息最终进入这里。
        return QueueBuilder.durable(COLD_OPERATION_DLQ).build();
    }

    /**
     * 绑定队列到交换机 - 冷数据操作
     */
    @Bean
    public Binding coldOperationBinding() {
        // 将冷操作路由键绑定到消费者实际监听的主队列。
        return BindingBuilder.bind(coldOperationQueue())
                .to(coldOperationExchange())
                .with(COLD_OPERATION_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到交换机 - 冷数据操作
     */
    @Bean
    public Binding coldOperationDLBinding() {
        // 将冷操作死信路由键绑定到死信队列。
        return BindingBuilder.bind(coldOperationDLQueue())
                .to(coldOperationExchange())
                .with(COLD_OPERATION_DL_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // MediaOperationEvent 和 MediaBindEvent 以 JSON 在不同服务间传输。
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // 连接参数由 application.yml 外部化，避免把环境地址写死在业务代码中。
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 统一消息格式，生产者无需手工调用 ObjectMapper。
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
