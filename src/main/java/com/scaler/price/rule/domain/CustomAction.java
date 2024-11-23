package com.scaler.price.rule.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;

@Entity

@EqualsAndHashCode(callSuper=false)
public class CustomAction extends RuleAction {
    private String actionName;
    private String actionType;
    private String actionValue;
    
    public CustomAction() {
        super();
    }

    public CustomAction(String actionName, String actionType, String actionValue) {
        this.actionName = actionName;
        this.actionType = actionType;
        this.actionValue = actionValue;
    }

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String script;
}
