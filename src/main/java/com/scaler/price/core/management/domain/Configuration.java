package com.scaler.price.core.management.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

@Entity
@Table(name = "configurations",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"key", "site_id"})
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Embedded
    private AuditInfo auditInfo;

    @Version
    private Long version;

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
