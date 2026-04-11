package com.techgadget.ecommerce;

import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Map;


@SpringBootApplication
@EnableJpaAuditing
public class EcommerceBackendApplication {

	static void main(String[] args) {
		SpringApplication.run(EcommerceBackendApplication.class, args);
	}

}
