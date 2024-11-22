package com.scaler.price.validation.services;

import com.scaler.price.core.management.exceptions.CategoryValidationException;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.scaler.price.rule.repository.CategoryRepository;
import com.scaler.price.rule.service.SiteService;
import io.micrometer.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class CategoryValidator {
    private final SiteService siteService;
    private final ConfigurationService configService;
    private final CategoryRepository categoryRepository;

    public void validateCategory(CategoryConstraints category) {
        validateBasicFields(category);
        validateHierarchy(category);
        validateSiteMappings(category);
        validateAttributes(category.getAttributes());
        validatePriceAttributes(category.getAttributes());
        validateUniqueness(category);
        validateCircularDependency(category);
    }

    public void validateCategoryUpdate(CategoryConstraints existingCategory, CategoryConstraints updatedCategory) {
        validateCategory(updatedCategory);
        validateUpdateSpecificRules(existingCategory, updatedCategory);
    }

    private void validateBasicFields(CategoryConstraints category) {
        if (StringUtils.isBlank(category.getCategoryId())) {
            throw new CategoryValidationException("Category ID is required");
        }

        if (!category.getCategoryId().matches("^[A-Z0-9_]{2,50}$")) {
            throw new CategoryValidationException(
                    "Category ID must be 2-50 characters long and contain only uppercase letters, numbers and underscores"
            );
        }

        if (StringUtils.isBlank(category.getCategoryName())) {
            throw new CategoryValidationException("Category name is required");
        }

        if (category.getCategoryName().length() > configService.getMaxCategoryNameLength()) {
            throw new CategoryValidationException(
                    "Category name exceeds maximum length of " +
                            configService.getMaxCategoryNameLength()
            );
        }

        if (category.getDisplayOrder() == null || category.getDisplayOrder() < 0) {
            throw new CategoryValidationException("Valid display order is required");
        }
    }

    private void validateHierarchy(CategoryConstraints category) {
        // Validate parent category if present
        if (category.getParentCategory() != null) {
            String parentId = category.getParentCategory().getCategoryId();
            CategoryConstraints parent = categoryRepository.findByCategoryId(parentId)
                    .orElseThrow(() -> new CategoryValidationException(
                            "Parent category not found: " + parentId
                    ));

            if (!parent.getIsActive()) {
                throw new CategoryValidationException(
                        "Parent category is not active: " + parentId
                );
            }

            // Validate level depth
            if (parent.getLevel() >= configService.getMaxCategoryDepth()) {
                throw new CategoryValidationException(
                        "Maximum category depth exceeded: " + configService.getMaxCategoryDepth()
                );
            }

            // Validate site mappings with parent
            if (!parent.getSiteIds().containsAll(category.getSiteIds())) {
                throw new CategoryValidationException(
                        "Category must be mapped to same or subset of parent's sites"
                );
            }
        }
    }

    private void validateSiteMappings(CategoryConstraints category) {
        if (category.getSiteIds() == null || category.getSiteIds().isEmpty()) {
            throw new CategoryValidationException("At least one site mapping is required");
        }

        for (String siteId : category.getSiteIds()) {
            if (!siteService.isValidSite(siteId)) {
                throw new CategoryValidationException("Invalid site ID: " + siteId);
            }

            if (!siteService.isSiteActive(siteId)) {
                throw new CategoryValidationException("Site is not active: " + siteId);
            }
        }

        if (category.getSiteIds().size() > configService.getMaxSiteMappingsPerCategory()) {
            throw new CategoryValidationException(
                    "Number of site mappings exceeds maximum allowed: " +
                            configService.getMaxSiteMappingsPerCategory()
            );
        }
    }

    private void validateAttributes(CategoryAttributes attributes) {
        if (attributes == null) {
            return;
        }

        // Validate custom attributes
        if (attributes.getCustomAttributes() != null) {
            validateCustomAttributes(attributes.getCustomAttributes());
        }

        // Validate validation rules
        if (attributes.getValidationRules() != null) {
            validateValidationRules(attributes.getValidationRules());
        }

        // Validate tags
        if (attributes.getTags() != null) {
            validateTags(attributes.getTags());
        }
    }

    private void validatePriceAttributes(CategoryAttributes attributes) {
        if (attributes == null || attributes.getPriceAttributes() == null) {
            return;
        }

        for (Map.Entry<String, CategoryAttributes.PriceAttribute> entry :
                attributes.getPriceAttributes().entrySet()) {
            CategoryAttributes.PriceAttribute priceAttr = entry.getValue();

            // Validate price ranges
            if (priceAttr.getMinimumPrice() != null &&
                    priceAttr.getMaximumPrice() != null) {
                if (priceAttr.getMinimumPrice().compareTo(priceAttr.getMaximumPrice()) > 0) {
                    throw new CategoryValidationException(
                            "Minimum price cannot be greater than maximum price for attribute: " +
                                    entry.getKey()
                    );
                }
            }

            // Validate margin ranges
            if (priceAttr.getMinimumMargin() != null &&
                    priceAttr.getMaximumMargin() != null) {
                if (priceAttr.getMinimumMargin().compareTo(priceAttr.getMaximumMargin()) > 0) {
                    throw new CategoryValidationException(
                            "Minimum margin cannot be greater than maximum margin for attribute: " +
                                    entry.getKey()
                    );
                }

                if (priceAttr.getMinimumMargin().compareTo(BigDecimal.ZERO) < 0 ||
                        priceAttr.getMaximumMargin().compareTo(new BigDecimal("100")) > 0) {
                    throw new CategoryValidationException(
                            "Margins must be between 0 and 100 percent for attribute: " +
                                    entry.getKey()
                    );
                }
            }
        }
    }

    private void validateCustomAttributes(Map<String, String> customAttributes) {
        if (customAttributes.size() > configService.getMaxCustomAttributesPerCategory()) {
            throw new CategoryValidationException(
                    "Number of custom attributes exceeds maximum allowed: " +
                            configService.getMaxCustomAttributesPerCategory()
            );
        }

        for (Map.Entry<String, String> entry : customAttributes.entrySet()) {
            if (!isValidAttributeKey(entry.getKey())) {
                throw new CategoryValidationException(
                        "Invalid custom attribute key: " + entry.getKey()
                );
            }

            if (entry.getValue() != null &&
                    entry.getValue().length() > configService.getMaxAttributeValueLength()) {
                throw new CategoryValidationException(
                        "Custom attribute value exceeds maximum length for key: " +
                                entry.getKey()
                );
            }
        }
    }

    private void validateValidationRules(
            Map<String, CategoryAttributes.ValidationRule> validationRules) {
        if (validationRules.size() > configService.getMaxValidationRulesPerCategory()) {
            throw new CategoryValidationException(
                    "Number of validation rules exceeds maximum allowed: " +
                            configService.getMaxValidationRulesPerCategory()
            );
        }

        for (Map.Entry<String, CategoryAttributes.ValidationRule> entry :
                validationRules.entrySet()) {
            CategoryAttributes.ValidationRule rule = entry.getValue();

            if (StringUtils.isBlank(rule.getRuleExpression())) {
                throw new CategoryValidationException(
                        "Rule expression is required for rule: " + entry.getKey()
                );
            }

            validateRuleExpression(rule.getRuleExpression());
        }
    }

    private void validateTags(Set<String> tags) {
        if (tags.size() > configService.getMaxTagsPerCategory()) {
            throw new CategoryValidationException(
                    "Number of tags exceeds maximum allowed: " +
                            configService.getMaxTagsPerCategory()
            );
        }

        for (String tag : tags) {
            if (!isValidTag(tag)) {
                throw new CategoryValidationException("Invalid tag: " + tag);
            }
        }
    }

    private void validateUniqueness(CategoryConstraints category) {
        // Check for duplicate category ID
        if (categoryRepository.existsById(category.getCategoryId())) {
            throw new CategoryValidationException(
                    "Category ID already exists: " + category.getCategoryId()
            );
        }

        // Check for duplicate category name within same parent
        if (category.getParentCategory() != null) {
            boolean hasDuplicateName = categoryRepository
                    .findByParentCategoryId(category.getParentCategory().getCategoryId())
                    .stream()
                    .anyMatch(c -> c.getCategoryName().equalsIgnoreCase(
                            category.getCategoryName()
                    ));

            if (hasDuplicateName) {
                throw new CategoryValidationException(
                        "Category name already exists under parent: " +
                                category.getCategoryName()
                );
            }
        }
    }

    private void validateCircularDependency(CategoryConstraints category) {
        Set<String> visitedCategories = new HashSet<>();
        CategoryConstraints current = category;

        while (current != null) {
            if (!visitedCategories.add(current.getCategoryId())) {
                throw new CategoryValidationException(
                        "Circular dependency detected in category hierarchy"
                );
            }
            current = current.getParentCategory();
        }
    }

    private void validateUpdateSpecificRules(CategoryConstraints existing, CategoryConstraints updated) {
        // Validate immutable fields
        if (!existing.getCategoryId().equals(updated.getCategoryId())) {
            throw new CategoryValidationException("Category ID cannot be changed");
        }

        // Validate hierarchy changes
        if (hasHierarchyChanged(existing, updated)) {
            validateHierarchyChange(existing, updated);
        }

        // Validate site mapping changes
        if (!existing.getSiteIds().equals(updated.getSiteIds())) {
            validateSiteMappingChange(existing, updated);
        }
    }

    private boolean hasHierarchyChanged(CategoryConstraints existing, CategoryConstraints updated) {
        return (existing.getParentCategory() == null && updated.getParentCategory() != null) ||
                (existing.getParentCategory() != null && updated.getParentCategory() == null) ||
                (existing.getParentCategory() != null && updated.getParentCategory() != null &&
                        !existing.getParentCategory().getCategoryId()
                                .equals(updated.getParentCategory().getCategoryId()));
    }

    private void validateHierarchyChange(CategoryConstraints existing, CategoryConstraints updated) {
        // Check if category has subcategories
        if (!existing.getSubCategories().isEmpty()) {
            validateSubcategoryMigration(existing, updated);
        }

        // Validate new parent if present
        if (updated.getParentCategory() != null) {
            validateNewParent(existing, updated.getParentCategory());
        }
    }

    private void validateSubcategoryMigration(CategoryConstraints existing, CategoryConstraints updated) {
        int newLevel = updated.getParentCategory() != null ?
                updated.getParentCategory().getLevel() + 1 : 1;

        for (CategoryConstraints subCategory : existing.getSubCategories()) {
            if (newLevel + getMaxDepth(subCategory) > configService.getMaxCategoryDepth()) {
                throw new CategoryValidationException(
                        "Moving category would exceed maximum depth for subcategory: " +
                                subCategory.getCategoryId()
                );
            }
        }
    }

    private int getMaxDepth(CategoryConstraints category) {
        if (category.getSubCategories().isEmpty()) {
            return 1;
        }

        return 1 + category.getSubCategories().stream()
                .mapToInt(this::getMaxDepth)
                .max()
                .orElse(0);
    }

    private boolean isValidAttributeKey(String key) {
        return key != null &&
                key.matches("^[a-zA-Z][a-zA-Z0-9_]{1,49}$");
    }

    private boolean isValidTag(String tag) {
        return tag != null &&
                tag.matches("^[a-zA-Z0-9-_]{1,30}$");
    }

    private void validateRuleExpression(String expression) {
        // Add custom rule expression validation logic
        // This could involve parsing and validating the expression syntax
        if (StringUtils.isBlank(expression)) {
            throw new CategoryValidationException("Rule expression cannot be empty");
        }
        // Add more complex validation as needed
    }
}
