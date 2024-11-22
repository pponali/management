package com.scaler.price.core.management.exceptions;

public class BulkUploadException extends RuntimeException {
    public BulkUploadException(String message) {
        super(message);
    }

    public BulkUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
