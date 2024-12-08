package com.scaler.price.core.management.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.scaler.price.core.management.mappers")
public class MapperConfig {
    // No additional configuration needed for this component scan

}