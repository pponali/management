package com.scaler.price.rule.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.*;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import lombok.Data;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "rule_versions")
@Data
public class RuleVersion extends AuditInfo{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ruleId;

    @Column(nullable = false)
    private Integer versionNumber;

    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private JsonNode ruleSnapshot;
}