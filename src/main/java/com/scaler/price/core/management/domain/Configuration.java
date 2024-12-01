package com.scaler.price.core.management.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "configurations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"key", "site_id"})
        }
)
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration extends AuditInfo{

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    @Column(name = "site_id")
    private String siteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigType type;

    private String description;

    @Column(nullable = false)
    private Boolean isEncrypted;

    @Column(nullable = false)
    private Boolean isMutable;

    @Column(nullable = false)
    private Boolean isActive;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    public enum ConfigType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON,
        DATE,
        TIME,
        DATETIME,
        ENCRYPTED
    }
}
