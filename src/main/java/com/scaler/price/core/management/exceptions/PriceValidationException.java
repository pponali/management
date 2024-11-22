package com.scaler.price.core.management.exceptions;

public class PriceValidationException extends Throwable {
    public PriceValidationException(String message) {
        super(message);
    }

    public PriceValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}