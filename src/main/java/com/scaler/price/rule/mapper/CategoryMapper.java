package com.scaler.price.rule.mapper;

import com.scaler.price.rule.domain.Category;
import com.scaler.price.rule.dto.CategoryAttributes;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryAttributes categoryToCategoryAttributes(Category category);
}
