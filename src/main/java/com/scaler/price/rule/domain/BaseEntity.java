package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import lombok.Data;
import org.springframework.data.annotation.Id;

import jakarta.persistence.GenerationType;

@Entity
@Data
public class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private AuditInfo auditInfo;
}
