package com.scaler.price.core.management.config;

import com.scaler.price.core.management.util.SqlScriptGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

@Configuration
public class DataInitializationConfig {

    @Autowired
    private SqlScriptGenerator sqlScriptGenerator;

    @Bean
    @Profile("!prod")  // Don't run in production
    @ConditionalOnProperty(name = "app.initialization.enabled", havingValue = "true", matchIfMissing = true)
    public CommandLineRunner initializeData(DataSource dataSource) {
        return args -> {
            // Generate SQL script from Excel
            sqlScriptGenerator.generateSqlScript();
            
            // Execute the generated SQL script
            ResourceDatabasePopulator resourceDatabasePopulator = new ResourceDatabasePopulator(false, false, "UTF-8");
            resourceDatabasePopulator.addScript(new ClassPathResource("db/migration/V2__Insert_Sample_Prices.sql"));
            resourceDatabasePopulator.execute(dataSource);
        };
    }
}
