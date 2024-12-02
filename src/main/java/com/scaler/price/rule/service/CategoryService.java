package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import com.scaler.price.rule.exceptions.CategoryNotFoundException;
import com.scaler.price.rule.repository.CategoryRepository;
import com.scaler.price.validation.services.CategoryValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public boolean isValidCategory(Long categoryId) {
        return categoryRepository.existsById(categoryId);
    }


    private final CategoryValidator categoryValidator;

    @Transactional(readOnly = true)
    public CategoryConstraints getCategory(Long categoryId) {
        return categoryRepository.findByCategoryId(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(
                        "Category not found: " + categoryId
                ));
    }

    @Transactional
    public CategoryConstraints createCategory(CategoryConstraints category) {
        categoryValidator.validateCategory(category);
        setupCategoryHierarchy(category);
        return categoryRepository.save(category);
    }

    @Transactional
    public CategoryConstraints updateCategory(Long categoryId, CategoryConstraints updatedCategory) {
        CategoryConstraints existingCategory = getCategory(categoryId);
        categoryValidator.validateCategoryUpdate(existingCategory, updatedCategory);
        updateCategoryFields(existingCategory, updatedCategory);
        return categoryRepository.save(existingCategory);
    }

    @Transactional(readOnly = true)
    public List<CategoryConstraints> getParentCategories(Long categoryId) {
        return categoryRepository.findParentCategories(categoryId);
    }

    @Transactional(readOnly = true)
    public List<CategoryConstraints> getAllSubCategories(Long categoryId) {
        return categoryRepository.findAllSubCategories(categoryId);
    }

    private void setupCategoryHierarchy(CategoryConstraints category) {
        if (category.getParentCategory() != null) {
            CategoryConstraints parent = getCategory(
                    category.getParentCategory().getCategoryId()
            );
            category.setLevel(parent.getLevel() + 1);
            category.setCategoryPath(
                    parent.getCategoryPath() + "/" + category.getCategoryId()
            );
        } else {
            category.setLevel(1);
            category.setCategoryPath("/" + category.getCategoryId());
        }
    }

    private void updateCategoryFields(
            CategoryConstraints existing,
            CategoryConstraints updated) {
        existing.setCategoryName(updated.getCategoryName());
        existing.setDescription(updated.getDescription());
        existing.setIsActive(updated.getIsActive());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setAttributes(updated.getAttributes());
        existing.setSiteIds(updated.getSiteIds());

        if (!Objects.equals(
                existing.getParentCategory(),
                updated.getParentCategory())) {
            updateCategoryParent(existing, updated.getParentCategory());
        }
    }

    private void updateCategoryParent(
            CategoryConstraints category,
            CategoryConstraints newParent) {
        if (category.getParentCategory() != null) {
            category.getParentCategory().removeSubCategory(category);
        }
        if (newParent != null) {
            newParent.addSubCategory(category);
            setupCategoryHierarchy(category);
        }
    }

    public boolean existsById(Long categoryId) {
        return categoryRepository.existsById(categoryId);
    }
}
