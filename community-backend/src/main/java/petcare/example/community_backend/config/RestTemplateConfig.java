package petcare.example.community_backend.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RestTemplateConfig {

    /**
     * 定义用于远程 HTTP 调用的 RestTemplate Bean
     */
    @Bean
    @LoadBalanced  // ✨ 关键就在这里！
    public org.springframework.web.client.RestTemplate restTemplate() {
        return new org.springframework.web.client.RestTemplate();
    }
}