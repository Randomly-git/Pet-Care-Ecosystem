package com.petcare.backend.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ActivityRecord 冷数据迁移 RabbitMQ 配置
 */
@Configuration
public class ActivityColdDataRabbitMQConfig {

    // ==================== 交换机 ====================
    public static final String COLD_MIGRATION_EXCHANGE = "petcare.activity.cold.migration.exchange";

    // ==================== 队列 ====================
    public static final String COLD_MIGRATION_QUEUE = "petcare.activity.cold.migration.queue";
    public static final String COLD_MIGRATION_DLQ = "petcare.activity.cold.migration.dlq";

    // ==================== 路由键 ====================
    public static final String COLD_MIGRATION_ROUTING_KEY = "activity.cold.migration";
    public static final String COLD_MIGRATION_DL_ROUTING_KEY = "activity.cold.migration.dl";


    // ==================== 2. BERT AI 分析配置 (新增) ====================
    // 发送给 Python BERT 服务的任务队列
    public static final String BERT_TASK_QUEUE = "pet_health_analysis_queue";
    // 从 Python 返回结果的队列
    public static final String BERT_RESULT_QUEUE = "pet_analysis_result_queue";

    /**
     * 声明 BERT 任务队列
     */
    @Bean
    public Queue bertTaskQueue() {
        return new Queue(BERT_TASK_QUEUE, true);
    }

    /**
     * 声明 BERT 结果队列
     */
    @Bean
    public Queue bertResultQueue() {
        return new Queue(BERT_RESULT_QUEUE, true);
    }

    /**
     * 声明冷数据迁移交换机
     */
    @Bean
    public DirectExchange coldMigrationExchange() {
        return new DirectExchange(COLD_MIGRATION_EXCHANGE, true, false);
    }

    /**
     * 声明冷数据迁移队列
     */
    @Bean
    public Queue coldMigrationQueue() {
        return QueueBuilder.durable(COLD_MIGRATION_QUEUE)
                .withArgument("x-dead-letter-exchange", COLD_MIGRATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", COLD_MIGRATION_DL_ROUTING_KEY)
                .build();
    }

    /**
     * 声明死信队列
     */
    @Bean
    public Queue coldMigrationDLQueue() {
        return QueueBuilder.durable(COLD_MIGRATION_DLQ).build();
    }

    /**
     * 绑定迁移队列到交换机
     */
    @Bean
    public Binding coldMigrationBinding() {
        return BindingBuilder.bind(coldMigrationQueue())
                .to(coldMigrationExchange())
                .with(COLD_MIGRATION_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到交换机
     */
    @Bean
    public Binding coldMigrationDLBinding() {
        return BindingBuilder.bind(coldMigrationDLQueue())
                .to(coldMigrationExchange())
                .with(COLD_MIGRATION_DL_ROUTING_KEY);
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
