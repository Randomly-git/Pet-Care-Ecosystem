package com.petcare.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetCareEcosystemBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(PetCareEcosystemBackendApplication.class, args);
	}

}
