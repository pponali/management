package com.scaler.price.rule.config;

import com.scaler.price.core.management.domain.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository("ruleConfigurationRepository")
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {
    
}