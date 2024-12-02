package com.scaler.price.rule.domain;

import com.scaler.price.core.management.domain.AuditInfo;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Table(name = "category_summary")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategorySummary extends AuditInfo {
    private Long categoryId;
    private String categoryName;
    private Long subCategoryCount;
    private String status;

}
