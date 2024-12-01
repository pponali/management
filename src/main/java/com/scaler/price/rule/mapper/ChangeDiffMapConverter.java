package com.scaler.price.rule.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.domain.ChangeDiff;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

@Converter
public class ChangeDiffMapConverter implements AttributeConverter<Map<String, ChangeDiff>, String> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(Map<String, ChangeDiff> attribute) {
        try {
            return attribute == null ? null : objectMapper.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting to JSON", e);
        }
    }

    @Override
    public Map<String, ChangeDiff> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null) return null;
            return objectMapper.readValue(dbData,
                    new TypeReference<Map<String, ChangeDiff>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Error converting from JSON", e);
        }
    }
}
