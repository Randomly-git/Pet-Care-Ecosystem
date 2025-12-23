// config/RestTemplateConfig.java
package com.petcare.backend.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    @LoadBalanced  // ✨ 关键就在这里！
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}