package com.petcare.media;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class MediaBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MediaBackendApplication.class, args);
	}

}
