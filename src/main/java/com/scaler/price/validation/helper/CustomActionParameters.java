package com.scaler.price.validation.helper;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Setter
@Getter
public class CustomActionParameters extends ActionParameters {
    private String actionType;
    private String actionValue;
    private Integer quantity;
    private Integer bundleQuantity;  // New field
    private String script;

    public CustomActionParameters(String actionType, String actionValue) {
        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    public CustomActionParameters(String actionType, String actionValue, Integer quantity) {
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.quantity = quantity;
    }

    // New constructor to include bundleQuantity
    public CustomActionParameters(String actionType, String actionValue, Integer quantity, Integer bundleQuantity) {
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.quantity = quantity;
        this.bundleQuantity = bundleQuantity;
    }

    public CustomActionParameters(String actionType, String actionValue, String script) {
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.script = script;
    }

    public CustomActionParameters(String actionType, String actionValue, Integer quantity, Integer bundleQuantity, String script) {
        this.actionType = actionType;
        this.actionValue = actionValue;
        this.quantity = quantity;
        this.bundleQuantity = bundleQuantity;
        this.script = script;
    }

    // Existing getters...

    // New getter for bundleQuantity
    public Integer getBundleQuantity() {
        return bundleQuantity;
    }

    // Optional setter for bundleQuantity
    public void setBundleQuantity(Integer bundleQuantity) {
        this.bundleQuantity = bundleQuantity;
    }

    // Getter for script
    public String getScript() {
        return script;
    }

    // Setter for script
    public void setScript(String script) {
        this.script = script;
    }

    // Add a method to get parameters as a map
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("actionType", this.actionType);
        parameters.put("actionValue", this.actionValue);
        
        if (this.quantity != null) {
            parameters.put("quantity", this.quantity);
        }
        
        if (this.bundleQuantity != null) {
            parameters.put("bundleQuantity", this.bundleQuantity);
        }
        
        if (this.script != null) {
            parameters.put("script", this.script);
        }
        
        return parameters;
    }
}