package com.scaler.price.validation.helper;

public class CustomActionParameters extends ActionParameters{
    private String actionType;
    private String actionValue;

    public CustomActionParameters(String actionType, String actionValue) {
        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    public String getActionType() {
        return actionType;
    }

    public String getActionValue() {
        return actionValue;
    }
}
