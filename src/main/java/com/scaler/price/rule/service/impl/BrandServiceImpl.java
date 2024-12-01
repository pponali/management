package com.scaler.price.rule.service.impl;

import com.scaler.price.rule.domain.Brand;
import com.scaler.price.rule.dto.BrandDTO;
import com.scaler.price.rule.mapper.BrandMapper;
import com.scaler.price.rule.repository.BrandRepository;
import com.scaler.price.rule.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;

    @Override
    @Transactional
    public BrandDTO create(BrandDTO brandDTO) {
        if (brandRepository.existsByNameIgnoreCase(brandDTO.getName())) {
            throw new IllegalArgumentException("Brand with name " + brandDTO.getName() + " already exists");
        }
        Brand brand = brandMapper.toEntity(brandDTO);
        return brandMapper.toDTO(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandDTO update(Long id, BrandDTO brandDTO) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));

        if (!brand.getName().equalsIgnoreCase(brandDTO.getName()) &&
                brandRepository.existsByNameIgnoreCase(brandDTO.getName())) {
            throw new IllegalArgumentException("Brand with name " + brandDTO.getName() + " already exists");
        }

        brandMapper.updateEntityFromDTO(brandDTO, brand);
        return brandMapper.toDTO(brandRepository.save(brand));
    }

    @Override
    public BrandDTO findById(Long id) {
        return brandRepository.findById(id)
                .map(brandMapper::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!brandRepository.existsById(id)) {
            throw new ResourceNotFoundException("Brand not found with id: " + id);
        }
        brandRepository.softDeleteById(id);
    }

    @Override
    public Page<BrandDTO> findAll(Pageable pageable) {
        return brandRepository.findAll(pageable)
                .map(brandMapper::toDTO);
    }

    @Override
    public List<BrandDTO> findByCategory(Long categoryId) {
        return brandRepository.findByCategoryId(categoryId)
                .stream()
                .map(brandMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<BrandDTO> searchByName(String namePattern) {
        return brandRepository.findByNameContainingIgnoreCase(namePattern, Pageable.unpaged())
                .stream()
                .map(brandMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return brandRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public List<BrandDTO> findPopularBrands(int limit) {
        return brandRepository.findPopularBrands(limit)
                .stream()
                .map(brandMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deactivateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brand.setActive(false);
        brandRepository.save(brand);
    }

    @Override
    @Transactional
    public void activateBrand(Long id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found with id: " + id));
        brand.setActive(true);
        brandRepository.save(brand);
    }

    @Override
    public boolean isValidBrand(Long brandId) {
        return brandRepository.existsById(brandId);
    }
}