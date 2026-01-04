package com.example.demo.config;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${rest-client.timeouts.connect-timeout:3000}")
    private int connectTimeout;

    @Value("${rest-client.timeouts.internal-read-timeout:5000}")
    private int internalReadTimeout;

    @Value("${rest-client.timeouts.ai-read-timeout:15000}")
    private int aiReadTimeout;

    /**
     * 1. 专门用于微服务内部调用（支持 Nacos 服务名）
     */
    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return createRestTemplate(internalReadTimeout);
    }

    /**
     * 2. 专门用于调用公网外部 API（如通义千问）
     */
    @Bean
    public RestTemplate normalRestTemplate() {
        return createRestTemplate(aiReadTimeout);
    }

    /**
     * 创建带超时配置的RestTemplate
     */
    private RestTemplate createRestTemplate(int readTimeout) {
        // 方式1：先创建RequestFactory再设置HttpClient
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();

        // 设置工厂的超时参数
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectTimeout(readTimeout);
        factory.setConnectionRequestTimeout(connectTimeout);

        // 创建HttpClient并设置给Factory
        CloseableHttpClient httpClient = HttpClients.createDefault();
        factory.setHttpClient((HttpClient) httpClient);

        return new RestTemplate(factory);
    }
}