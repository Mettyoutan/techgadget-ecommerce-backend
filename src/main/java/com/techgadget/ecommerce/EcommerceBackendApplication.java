package com.techgadget.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class EcommerceBackendApplication {

	static void main(String[] args) {
		SpringApplication.run(EcommerceBackendApplication.class, args);
	}

}
