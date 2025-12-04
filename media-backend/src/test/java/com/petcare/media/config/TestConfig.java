// src/test/java/com/petcare/media/config/TestConfig.java
package com.petcare.media.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class TestConfig {

    @Bean
    public RestTemplate testRestTemplate() {
        // Spring Boot 3.x 会自动配置合适的 RestTemplate
        return new RestTemplate();
    }
}