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

    // 本类由 Spring 扫描并实例化；下面的 Bean 会在应用启动时自动声明 RabbitMQ 拓扑。

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
        // durable=true 表示 RabbitMQ 重启后仍保留队列；任务消息不会因 Broker 重启而丢失。
        return new Queue(BERT_TASK_QUEUE, true);
    }

    /**
     * 声明 BERT 结果队列
     */
    @Bean
    public Queue bertResultQueue() {
        // Java 服务和 Python BERT 服务通过这个持久化结果队列完成异步回传。
        return new Queue(BERT_RESULT_QUEUE, true);
    }

    /**
     * 声明冷数据迁移交换机
     */
    @Bean
    public DirectExchange coldMigrationExchange() {
        // DirectExchange 按 routing key 精确匹配队列，适合区分“正常迁移”和“死信”。
        // 第二个参数 true 表示交换机持久化，第三个参数 false 表示不自动删除。
        return new DirectExchange(COLD_MIGRATION_EXCHANGE, true, false);
    }

    /**
     * 声明冷数据迁移队列
     */
    @Bean
    public Queue coldMigrationQueue() {
        // 主队列持久化，并把处理失败的消息转发回同一交换机的死信路由键。
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
        // 死信队列保存多次处理仍失败的迁移任务，便于人工排查和补偿。
        return QueueBuilder.durable(COLD_MIGRATION_DLQ).build();
    }

    /**
     * 绑定迁移队列到交换机
     */
    @Bean
    public Binding coldMigrationBinding() {
        // 将正常迁移路由键绑定到主队列；生产者只需发送交换机和该路由键。
        return BindingBuilder.bind(coldMigrationQueue())
                .to(coldMigrationExchange())
                .with(COLD_MIGRATION_ROUTING_KEY);
    }

    /**
     * 绑定死信队列到交换机
     */
    @Bean
    public Binding coldMigrationDLBinding() {
        // 将死信路由键绑定到死信队列，形成失败消息的落点。
        return BindingBuilder.bind(coldMigrationDLQueue())
                .to(coldMigrationExchange())
                .with(COLD_MIGRATION_DL_ROUTING_KEY);
    }

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        // 将 Java DTO 自动序列化为 JSON，避免生产者和消费者手工拼装字节数组。
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitTemplate 配置
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        // ConnectionFactory 使用 application.yml 中的 host、port、账号和虚拟主机创建连接。
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        // 让所有 RabbitTemplate.convertAndSend 调用默认使用上面的 JSON 转换器。
        template.setMessageConverter(jsonMessageConverter());
        // 返回模板，业务服务通过它发布冷迁移和 AI 分析消息。
        return template;
    }
}
