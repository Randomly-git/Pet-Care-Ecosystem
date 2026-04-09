package com.petcare.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Test 配置属性类
 * 从 program-config.yml 中读取测试相关配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "test")
public class TestProperties {

    /**
     * 是否模拟故障（用于白盒测试）
     */
    private boolean simulateFailure = false;
}