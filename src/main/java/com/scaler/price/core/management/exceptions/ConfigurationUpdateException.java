package com.scaler.price.core.management.exceptions;

public class ConfigurationUpdateException extends RuntimeException {
    public ConfigurationUpdateException(String message) {
        super(message);
    }
    
    public ConfigurationUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
