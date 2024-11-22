package com.scaler.price.rule.exceptions;

public class ActionExecutionException extends Throwable {
    public ActionExecutionException(String s) {
        super(s);
    }
    public ActionExecutionException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
