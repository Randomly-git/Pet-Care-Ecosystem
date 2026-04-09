package com.petcare.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Canal 配置属性类
 * 从 program-config.yml 中读取 Canal 相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "canal")
public class CanalProperties {

    /**
     * Canal 服务器地址
     */
    private String host;

    /**
     * Canal 服务器端口
     */
    private int port;

    /**
     * Canal 实例名称
     */
    private String destination;

    /**
     * 批量处理大小
     */
    private int batchSize;

    /**
     * 过滤器配置
     */
    private String filter;
}