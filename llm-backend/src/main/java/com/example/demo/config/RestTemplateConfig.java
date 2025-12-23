package com.example.demo.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // 这个用于微服务间调用 (petcare-backend)
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return new RestTemplate();
    }

    // 这个用于调用阿里大模型等外部接口
    @Bean
    public RestTemplate normalRestTemplate() {
        return new RestTemplate();
    }
}