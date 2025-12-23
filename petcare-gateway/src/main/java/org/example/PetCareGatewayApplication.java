package org.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Pet Care 微服务网关启动类
 */
@SpringBootApplication
@EnableDiscoveryClient // 开启服务发现功能，使其能够从 Nacos 获取后端服务的动态端口
public class PetCareGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(PetCareGatewayApplication.class, args);
        System.out.println("--- PetCare 网关启动成功，固定端口：9000 ---");
    }
}