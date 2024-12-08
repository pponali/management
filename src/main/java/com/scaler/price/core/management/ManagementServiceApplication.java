package com.scaler.price.core.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@SpringBootApplication(scanBasePackages = "com.scaler.price")
@OpenAPIDefinition(
    info = @Info(
        title = "Price Management API",
        version = "1.0.0",
        description = "API for managing prices and related operations"
    )
)
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = {
    "com.scaler.price.core.management.repository",
    "com.scaler.price.rule.repository",
    "com.scaler.price.audit.repository"
})
@EntityScan(basePackages = {
    "com.scaler.price.core.management.domain",
    "com.scaler.price.rule.domain",
    "com.scaler.price.rule.domain.constraint",
    "com.scaler.price.audit.domain"
})
public class ManagementServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ManagementServiceApplication.class, args);
    }
}
