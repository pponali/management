package com.scaler.price.rule.service;

import com.scaler.price.rule.domain.ProductAttribute;
import com.scaler.price.rule.exceptions.AttributeValidationException;
import org.springframework.stereotype.Service;

@Service
public class AttributeValidationService {
    
    public void validateAttribute(ProductAttribute attribute) throws AttributeValidationException {
        if (attribute == null) {
            throw new AttributeValidationException("Attribute cannot be null");
        }
        
        if (attribute.getProductId() == null) {
            throw new AttributeValidationException("Product ID is required");
        }
        
        if (attribute.getAttributeKey() == null) {
            throw new AttributeValidationException("Attribute key is required");
        }
        
        if (attribute.getAttributeValue() == null) {
            throw new AttributeValidationException("Attribute value is required");
        }
    }
    
    public void validateAttributeUpdate(ProductAttribute existingAttribute, ProductAttribute updatedAttribute) 
            throws AttributeValidationException {
        validateAttribute(updatedAttribute);
        
        if (!existingAttribute.getProductId().equals(updatedAttribute.getProductId())) {
            throw new AttributeValidationException("Product ID cannot be changed");
        }
        
        if (!existingAttribute.getAttributeKey().equals(updatedAttribute.getAttributeKey())) {
            throw new AttributeValidationException("Attribute key cannot be changed");
        }
    }
}
