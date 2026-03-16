package com.petcare.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableCaching // 👈 必须有这个，缓存才会生效
@EnableTransactionManagement
@EnableScheduling // 启用定时任务
public class MediaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaBackendApplication.class, args);
	}

}
