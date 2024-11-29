package com.scaler.price.core.management.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.core.management.domain.Configuration;
import com.scaler.price.core.management.exceptions.ConfigurationNotFoundException;
import com.scaler.price.core.management.exceptions.ConfigurationUpdateException;
import com.scaler.price.core.management.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.naming.ConfigurationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "configurations")
public class ConfigurationService {
    private final ConfigurationRepository configRepository;
    private final EncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    @Cacheable(key = "#key + '_' + #siteId")
    public String getValue(String key, String siteId) {
        Configuration config = configRepository
                .findByKeyAndSiteIdAndIsActive(key, siteId, true)
                .orElseGet(() -> getDefaultConfig(key));

        if (config == null) {
            return null;
        }

        return config.getIsEncrypted() ?
                decryptValue(config.getValue()) :
                config.getValue();
    }

    @Cacheable(key = "#key + '_' + #siteId + '_int'")
    public Integer getIntValue(String key, String siteId, Integer defaultValue) {
        String value = getValue(key, siteId);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Invalid integer value for key: {}", key);
            return defaultValue;
        }
    }

    @Cacheable(key = "#key + '_' + #siteId + '_decimal'")
    public BigDecimal getBigDecimalValue(
            String key,
            String siteId,
            BigDecimal defaultValue) {
        String value = getValue(key, siteId);
        try {
            return value != null ? new BigDecimal(value) : defaultValue;
        } catch (NumberFormatException e) {
            log.warn("Invalid decimal value for key: {}", key);
            return defaultValue;
        }
    }

    @Cacheable(key = "#key + '_' + #siteId + '_bool'")
    public Boolean getBooleanValue(String key, String siteId, Boolean defaultValue) {
        String value = getValue(key, siteId);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    @Cacheable(key = "#key + '_' + #siteId + '_json'")
    public <T> T getJsonValue(
            String key,
            String siteId,
            Class<T> valueType,
            T defaultValue) {
        String value = getValue(key, siteId);
        if (value == null) {
            return defaultValue;
        }

        try {
            return objectMapper.readValue(value, valueType);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JSON configuration: {}", e.getMessage());
            return defaultValue;
        }
    }

    @CacheEvict(key = "#key + '_' + #siteId")
    @Transactional
    public void setValue(String key, String value, Long siteId) {
        try {
            Configuration config = configRepository
                    .findByKeyAndSiteId(key, siteId)
                    .orElseThrow(() -> new ConfigurationNotFoundException(
                            "Configuration not found: " + key
                    ));

            if (!config.getIsMutable()) {
                throw new ConfigurationUpdateException("Configuration is not mutable: " + key);
            }

            validateValue(value, config.getType());

            String valueToStore = config.getIsEncrypted() ?
                    encryptValue(value) :
                    value;

            int updated = configRepository.updateConfigurationValue(
                    key,
                    valueToStore,
                    siteId
            );

            if (updated == 0) {
                throw new ConfigurationUpdateException(
                        "Failed to update configuration: " + key
                );
            }
        } catch (ConfigurationException e) {
            throw new ConfigurationUpdateException(e.getMessage(), e);
        }
    }

    

    @Transactional(readOnly = true)
    public Map<String, String> getAllConfigurations(Long siteId) {
        return configRepository.findActiveConfigurations(siteId)
                .stream()
                .collect(Collectors.toMap(
                        Configuration::getKey,
                        this::getConfigValue,
                        (v1, v2) -> v2  // Keep site-specific value
                ));
    }

    private Configuration getDefaultConfig(String key) {
        return configRepository
                .findByKeyAndSiteIdAndIsActive(key, null, true)
                .orElse(null);
    }

    private String getConfigValue(Configuration config) {
        return config.getIsEncrypted() ?
                decryptValue(config.getValue()) :
                config.getValue();
    }

    private void validateValue(String value, Configuration.ConfigType type) throws ConfigurationException {
        if (value == null) {
            throw new ConfigurationException("Value cannot be null");
        }

        try {
            switch (type) {
                case NUMBER -> new BigDecimal(value);
                case BOOLEAN -> Boolean.parseBoolean(value);
                case JSON -> objectMapper.readTree(value);
                case DATE -> LocalDate.parse(value);
                case TIME -> LocalTime.parse(value);
                case DATETIME -> LocalDateTime.parse(value);
            }
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Invalid value for type " + type + ": " + e.getMessage()
            );
        }
    }

    private String encryptValue(String value) throws ConfigurationException {
        try {
            return encryptionService.encrypt(value);
        } catch (Exception e) {
            throw new ConfigurationException(
                    "Error encrypting value: " + e.getMessage()
            );
        }
    }

    private String decryptValue(String value) throws ConfigurationNotFoundException {
        try {
            return encryptionService.decrypt(value);
        } catch (Exception e) {
            throw new ConfigurationNotFoundException(
                    "Error decrypting value: " + e.getMessage()
            );
        }
    }

    // Default configuration values
    public static final class Defaults {
        public static final int MAX_RULE_NAME_LENGTH = 100;
        public static final int MAX_RULE_DURATION_DAYS = 365;
        public static final int MAX_CONDITIONS_PER_RULE = 10;
        public static final int MAX_ACTIONS_PER_RULE = 5;
        public static final int MAX_CATEGORY_NAME_LENGTH = 100;
        public static final int MAX_CATEGORY_DEPTH = 5;
        public static final int MAX_SITE_MAPPINGS_PER_CATEGORY = 10;
        public static final int MAX_CUSTOM_ATTRIBUTES_PER_CATEGORY = 20;
        public static final int MAX_VALIDATION_RULES_PER_CATEGORY = 10;
        public static final int MAX_TAGS_PER_CATEGORY = 20;
        public static final int MAX_ATTRIBUTE_VALUE_LENGTH = 500;
    }
}