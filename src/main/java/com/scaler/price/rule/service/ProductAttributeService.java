package com.scaler.price.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.AttributeType;
import com.scaler.price.rule.domain.ProductAttribute;
import com.scaler.price.rule.events.AttributeEventPublisher;
import com.scaler.price.rule.exceptions.AttributeValidationException;
import com.scaler.price.rule.repository.ProductAttributeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductAttributeService {
    private final ProductAttributeRepository attributeRepository;
    private final ProductService productService;
    private final AttributeValidationService validationService;
    private final AttributeEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Transactional
    public ProductAttribute createAttribute(ProductAttribute attribute) throws AttributeValidationException {
        log.info("Creating attribute for product: {} key: {}",
                attribute.getProductId(), attribute.getAttributeKey());

        // Verify product exists and is active
        if (!productService.isProductActive(attribute.getProductId())) {
            throw new AttributeValidationException(
                    "Product not active or not found: " + attribute.getProductId()
            );
        }

        // Validate attribute
        validationService.validateAttribute(attribute);

        // Check for duplicate
        Optional<ProductAttribute> existing = attributeRepository
                .findByProductIdAndAttributeKey(
                        attribute.getProductId(),
                        attribute.getAttributeKey()
                );

        if (existing.isPresent()) {
            throw new AttributeValidationException(
                    "Attribute already exists: " + attribute.getAttributeKey()
            );
        }

        attribute.setCreatedAt(LocalDateTime.now());
        ProductAttribute savedAttribute = attributeRepository.save(attribute);

        eventPublisher.publishAttributeCreated(savedAttribute);
        return savedAttribute;
    }

    @Transactional
    public ProductAttribute updateAttribute(
            Long productId,
            Long attributeKey,
            ProductAttribute updatedAttribute) throws AttributeValidationException {

        log.info("Updating attribute for product: {} key: {}",
                productId, attributeKey);

        ProductAttribute existingAttribute = getAttributeOrThrow(
                productId,
                attributeKey
        );

        validationService.validateAttributeUpdate(
                existingAttribute,
                updatedAttribute
        );

        updateAttributeFields(existingAttribute, updatedAttribute);
        ProductAttribute savedAttribute = attributeRepository.save(existingAttribute);

        eventPublisher.publishAttributeUpdated(savedAttribute);
        return savedAttribute;
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "productAttributes", key = "#productId")
    public Map<Long, Object> getAttributes(Long productId) {
        log.debug("Fetching attributes for product: {}", productId);

        List<ProductAttribute> attributes = attributeRepository
                .findByProductId(productId);

        return convertToMap(attributes);
    }

    @Transactional(readOnly = true)
    public Map<Long, Object> getAttributesByCategory(
            Long productId,
            Long category) {

        log.debug("Fetching {} attributes for product: {}",
                category, productId);

        List<ProductAttribute> attributes = attributeRepository
                .findByProductIdAndCategory(productId, category);

        return convertToMap(attributes);
    }

    @Transactional
    @CacheEvict(value = "productAttributes", key = "#productId")
    public void deleteAttribute(Long productId, Long attributeKey) throws AttributeValidationException {
        log.info("Deleting attribute for product: {} key: {}",
                productId, attributeKey);

        ProductAttribute attribute = getAttributeOrThrow(productId, attributeKey);
        attributeRepository.delete(attribute);

        eventPublisher.publishAttributeDeleted(attribute);
    }

    @Transactional
    public void bulkUpdateAttributes(
            Long productId,
            Map<Long, Object> attributes) throws AttributeValidationException {

        log.info("Bulk updating attributes for product: {}", productId);

        // Verify product exists and is active
        if (!productService.isProductActive(productId)) {
            throw new AttributeValidationException(
                    "Product not active or not found: " + productId
            );
        }

        List<ProductAttribute> attributesToSave = new ArrayList<>();

        for (Map.Entry<Long, Object> entry : attributes.entrySet()) {
            ProductAttribute attribute = createAttributeFromEntry(
                    productId,
                    entry.getKey(),
                    entry.getValue()
            );

            validationService.validateAttribute(attribute);
            attributesToSave.add(attribute);
        }

        attributeRepository.saveAll(attributesToSave);
        eventPublisher.publishAttributesBulkUpdated(productId, attributesToSave);
    }

    @Transactional(readOnly = true)
    public List<Long> getFilterableAttributes(Long category) {
        return attributeRepository.findFilterableAttributesByCategory(category);
    }

    @Transactional(readOnly = true)
    public Map<Long, Object> getSpecificAttributes(
            Long productId,
            Set<Long> attributeKeys) {

        List<ProductAttribute> attributes = attributeRepository
                .findByProductIdAndAttributeKeys(productId, attributeKeys);

        return convertToMap(attributes);
    }

    private ProductAttribute getAttributeOrThrow(
            Long productId,
            Long attributeKey) throws AttributeValidationException {

        return attributeRepository
                .findByProductIdAndAttributeKey(productId, attributeKey)
                .orElseThrow(() -> new AttributeValidationException(
                        "Attribute not found: " + attributeKey
                ));
    }

    private void updateAttributeFields(
            ProductAttribute existing,
            ProductAttribute updated) {

        existing.setAttributeValue(updated.getAttributeValue());
        existing.setAttributeType(updated.getAttributeType());
        existing.setCategory(updated.getCategory());
        existing.setSubCategory(updated.getSubCategory());
        existing.setIsSearchable(updated.getIsSearchable());
        existing.setIsFilterable(updated.getIsFilterable());
        existing.setDisplayOrder(updated.getDisplayOrder());
        existing.setUpdatedAt(LocalDateTime.now());
    }

    private Map<Long, Object> convertToMap(List<ProductAttribute> attributes) {
        Map<Long, Object> result = new HashMap<>();

        for (ProductAttribute attribute : attributes) {
            Object value = convertAttributeValue(
                    attribute.getAttributeValue(),
                    attribute.getAttributeType()
            );
            result.put(attribute.getAttributeKey(), value);
        }

        return result;
    }

    private Object convertAttributeValue(String value, String type) {
        if (value == null) return null;

        try {
            return switch (AttributeType.valueOf(type)) {
                case NUMBER -> Double.parseDouble(value);
                case BOOLEAN -> Boolean.parseBoolean(value);
                case JSON -> objectMapper.readValue(value, Map.class);
                case DATE -> LocalDateTime.parse(value);
                default -> value;
            };
        } catch (Exception e) {
            log.error("Error converting attribute value: {}", e.getMessage());
            return value;
        }
    }

    private ProductAttribute createAttributeFromEntry(
            Long productId,
            Long key,
            Object value) throws AttributeValidationException {

        return ProductAttribute.builder()
                .productId(productId)
                .attributeKey(key)
                .attributeValue(convertToString(value))
                .attributeType(determineAttributeType(value))
                .createdAt(LocalDateTime.now())
                .isSearchable(true)
                .isFilterable(true)
                .build();
    }

    private String convertToString(Object value) throws AttributeValidationException {
        if (value == null) return null;

        if (value instanceof Map || value instanceof List) {
            try {
                return objectMapper.writeValueAsString(value);
            } catch (Exception e) {
                throw new AttributeValidationException(
                        "Error converting complex value to string"
                );
            }
        }

        return value.toString();
    }

    private String determineAttributeType(Object value) {
        if (value == null) return AttributeType.TEXT.name();

        return switch (value.getClass().getSimpleName()) {
            case "Integer", "Long", "Double", "BigDecimal" ->
                    AttributeType.NUMBER.name();
            case "Boolean" -> AttributeType.BOOLEAN.name();
            case "LinkedHashMap", "HashMap" -> AttributeType.JSON.name();
            default -> AttributeType.TEXT.name();
        };
    }

    public String getAttributeValue(Long productId, Long attribute) {
        return attributeRepository.findByProductIdAndAttributeKey(productId, attribute)
                .map(ProductAttribute::getAttributeValue)
                .orElse(null);
    }
}
