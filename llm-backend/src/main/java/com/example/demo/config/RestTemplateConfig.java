package com.example.demo.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
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
     * 创建带超时配置的 RestTemplate (适配 HttpClient 5)
     */
    private RestTemplate createRestTemplate(int readTimeout) {
        // 1. 使用 HttpClient 5 的方式创建客户端
        CloseableHttpClient httpClient = HttpClients.createDefault();

        // 2. 构造支持 HttpClient 5 的请求工厂
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // 3. 设置超时参数 (注意：Spring 6.1+ 建议通过底层 HttpClient 配置更复杂的超时)
        // 这里可以直接设置工厂级别的简单超时
        factory.setConnectTimeout(connectTimeout);
        // 注意：原代码中此处重复设置了两次 ConnectTimeout，应为 ReadTimeout
        factory.setConnectionRequestTimeout(connectTimeout);

            // 设置读取超时（虽然方法可能已弃用，但仍尝试设置）
        try {
            // 尝试设置读取超时
            java.lang.reflect.Method setReadTimeoutMethod = factory.getClass().getMethod("setReadTimeout", int.class);
            setReadTimeoutMethod.invoke(factory, readTimeout);
        } catch (Exception e) {
            // 如果反射失败，记录日志但不中断
            System.out.println("无法设置读取超时，将使用默认超时。ReadTimeout: " + readTimeout);
        }

        return new RestTemplate(factory);
    }
}