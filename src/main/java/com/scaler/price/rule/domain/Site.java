package com.scaler.price.rule.domain;

import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Type;

import jakarta.persistence.GenerationType;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "sites")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String siteId;

    @Column(nullable = false)
    private String siteName;

    @Column(nullable = false)
    private String siteUrl;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String locale;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @ElementCollection
    @CollectionTable(
        name = "site_category_mappings",
        joinColumns = @JoinColumn(name = "site_id")
    )
    @Column(name = "category_id")
    @Builder.Default
    private Set<Long> categoryIds = new HashSet<>();

    @ElementCollection
    @CollectionTable(
        name = "site_brand_mappings",
        joinColumns = @JoinColumn(name = "site_id")
    )
    @Column(name = "brand_id")
    @Builder.Default
    private Set<Long> brandIds = new HashSet<>();

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(length = 1000)
    private String description;

    @Version
    private Long version;
}
