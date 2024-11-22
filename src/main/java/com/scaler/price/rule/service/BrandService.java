package com.scaler.price.rule.service;

import com.scaler.price.rule.dto.BrandDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;


public interface BrandService {
    BrandDTO create(BrandDTO brandDTO);
    BrandDTO update(Long id, BrandDTO brandDTO);
    BrandDTO findById(Long id);
    void delete(Long id);
    Page<BrandDTO> findAll(Pageable pageable);
    List<BrandDTO> findByCategory(Long categoryId);
    List<BrandDTO> searchByName(String namePattern);
    boolean existsByName(String name);
    List<BrandDTO> findPopularBrands(int limit);
    void deactivateBrand(Long id);
    void activateBrand(Long id);
    boolean isValidBrand(String brandId);
}
