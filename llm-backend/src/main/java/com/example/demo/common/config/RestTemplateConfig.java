package com.example.demo.common.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
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

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate() {
        return createRestTemplate(internalReadTimeout);
    }

    @Bean
    public RestTemplate normalRestTemplate() {
        return createRestTemplate(aiReadTimeout);
    }

    private RestTemplate createRestTemplate(int readTimeout) {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        factory.setConnectTimeout(connectTimeout);
        factory.setConnectionRequestTimeout(connectTimeout);
        try {
            java.lang.reflect.Method m = factory.getClass().getMethod("setReadTimeout", int.class);
            m.invoke(factory, readTimeout);
        } catch (Exception e) {
            System.out.println("setReadTimeout failed, using default. timeout=" + readTimeout);
        }
        return new RestTemplate(factory);
    }
}
