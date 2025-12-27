package com.example.demo.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 1. 专门用于微服务内部调用（支持 Nacos 服务名）
     * 注入时请使用: @Qualifier("loadBalancedRestTemplate")
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    /**
     * 2. 专门用于调用公网外部 API（如通义千问）
     * 注入时请使用: @Qualifier("normalRestTemplate")
     */
    @Bean
    public RestTemplate normalRestTemplate() {
        return new RestTemplate();
    }
}