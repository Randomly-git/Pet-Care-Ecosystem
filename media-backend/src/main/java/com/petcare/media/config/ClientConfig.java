package com.petcare.media.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 客户端配置类，用于定义 RestTemplate 和 ObjectMapper 等 Bean。
 */
@Configuration
public class ClientConfig {

    /**
     * 定义用于远程 HTTP 调用的 RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        // 生产环境中，您可能需要在这里配置超时、拦截器等
        return new RestTemplate();
    }

    /**
     * 定义用于 JSON 序列化/反序列化的 ObjectMapper Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 JSR310 模块，支持 Java 8 时间类型 (LocalDateTime, etc.)
        mapper.registerModule(new JavaTimeModule());
        // 禁用将日期序列化为时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}