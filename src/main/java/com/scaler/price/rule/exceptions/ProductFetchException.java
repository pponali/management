package com.scaler.price.rule.exceptions;

import org.springframework.dao.DataAccessException;

public class ProductFetchException extends Throwable {
    public ProductFetchException(String unableToRetrieveProductIDs, DataAccessException e) {
        super(unableToRetrieveProductIDs, e);
    }
    public ProductFetchException(String s, Throwable throwable) {
        super(s, throwable);
    }
    public ProductFetchException(String s) {
        super(s);
    }
}
