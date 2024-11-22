package com.scaler.price.rule.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ValidationException;
import lombok.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationRules {
    private String type;
    private String format;
    private String pattern;
    private Boolean required;
    private String minimum;
    private String maximum;
    private Integer minLength;
    private Integer maxLength;
    private Integer minItems;
    private Integer maxItems;
    private Boolean uniqueItems;
    private Integer minProperties;
    private Integer maxProperties;
    private String itemType;
    private List<String> enumValues;
    private Map<String, ValidationRules> properties;
    private Map<String, ValidationRules> patternProperties;
    private ValidationRules itemSchema;
    private Boolean additionalProperties;
    private List<String> requiredProperties;
    private Map<String, Object> metadata;

    @Getter(lazy = true)
    private final JsonSchemaValidator validator = initializeValidator();

    public void validate(JsonNode value) throws ValidationException {
        try {
            List<String> errors = new ArrayList<>();

            // Type validation
            if (type != null && validateType(value, type)) {
                errors.add("Invalid type. Expected: " + type);
            }

            // Format validation
            if (format != null && !validateFormat(value, format)) {
                errors.add("Invalid format. Expected: " + format);
            }

            // Pattern validation
            if (pattern != null && !validatePattern(value, pattern)) {
                errors.add("Value does not match pattern: " + pattern);
            }

            // Numeric validations
            if (value.isNumber()) {
                validateNumericConstraints(value, errors);
            }

            // String validations
            if (value.isTextual()) {
                validateStringConstraints(value, errors);
            }

            // Array validations
            if (value.isArray()) {
                validateArrayConstraints(value, errors);
            }

            // Object validations
            if (value.isObject()) {
                validateObjectConstraints(value, errors);
            }

            // Enum validations
            if (enumValues != null && !enumValues.isEmpty()) {
                validateEnum(value, errors);
            }

            if (!errors.isEmpty()) {
                throw new ValidationException(String.join(", ", errors));
            }

        } catch (ValidationException | IllegalArgumentException e) {
            throw new ValidationException("Validation failed: " + e.getMessage());
        }
    }

    private boolean validateType(JsonNode value, String expectedType) {
        return !switch (expectedType.toUpperCase()) {
            case "STRING" -> value.isTextual();
            case "NUMBER" -> value.isNumber();
            case "INTEGER" -> value.isInt();
            case "BOOLEAN" -> value.isBoolean();
            case "ARRAY" -> value.isArray();
            case "OBJECT" -> value.isObject();
            default -> false;
        };
    }

    private boolean validateFormat(JsonNode value, String format) {
        if (!value.isTextual()) return false;
        String stringValue = value.asText();

        return switch (format.toLowerCase()) {
            case "date-time" -> validateDateTime(stringValue);
            case "date" -> validateDate(stringValue);
            case "time" -> validateTime(stringValue);
            case "email" -> validateEmail(stringValue);
            case "uri" -> validateUri(stringValue);
            case "uuid" -> validateUuid(stringValue);
            case "ipv4" -> validateIpv4(stringValue);
            case "ipv6" -> validateIpv6(stringValue);
            default -> true;
        };
    }

    private boolean validatePattern(JsonNode value, String pattern) {
        if (!value.isTextual()) return false;
        return value.asText().matches(pattern);
    }

    private void validateNumericConstraints(JsonNode value, List<String> errors) {
        BigDecimal numValue = value.decimalValue();

        if (minimum != null) {
            BigDecimal min = new BigDecimal(minimum);
            if (numValue.compareTo(min) < 0) {
                errors.add("Value must be greater than or equal to " + minimum);
            }
        }

        if (maximum != null) {
            BigDecimal max = new BigDecimal(maximum);
            if (numValue.compareTo(max) > 0) {
                errors.add("Value must be less than or equal to " + maximum);
            }
        }
    }

    private void validateStringConstraints(JsonNode value, List<String> errors) {
        String stringValue = value.asText();

        if (minLength != null && stringValue.length() < minLength) {
            errors.add("String length must be at least " + minLength);
        }

        if (maxLength != null && stringValue.length() > maxLength) {
            errors.add("String length must be at most " + maxLength);
        }
    }

    private void validateArrayConstraints(JsonNode value, List<String> errors) {
        if (minItems != null && value.size() < minItems) {
            errors.add("Array must contain at least " + minItems + " items");
        }

        if (maxItems != null && value.size() > maxItems) {
            errors.add("Array must contain at most " + maxItems + " items");
        }

        if (Boolean.TRUE.equals(uniqueItems)) {
            Set<String> items = new HashSet<>();
            for (JsonNode item : value) {
                if (!items.add(item.toString())) {
                    errors.add("Array must contain unique items");
                    break;
                }
            }
        }

        if (itemSchema != null) {
            for (JsonNode item : value) {
                try {
                    itemSchema.validate(item);
                } catch (ValidationException e) {
                    errors.add("Array item validation failed: " + e.getMessage());
                }
            }
        }

        if (itemType != null) {
            for (JsonNode item : value) {
                if (validateType(item, itemType)) {
                    errors.add("Array items must be of type: " + itemType);
                    break;
                }
            }
        }
    }

    private void validateObjectConstraints(JsonNode value, List<String> errors) {
        // Required properties
        if (requiredProperties != null) {
            for (String required : requiredProperties) {
                if (!value.has(required)) {
                    errors.add("Missing required property: " + required);
                }
            }
        }

        // Property count constraints
        if (minProperties != null && value.size() < minProperties) {
            errors.add("Object must have at least " + minProperties + " properties");
        }

        if (maxProperties != null && value.size() > maxProperties) {
            errors.add("Object must have at most " + maxProperties + " properties");
        }

        // Property validations
        if (properties != null) {
            value.fields().forEachRemaining(entry -> {
                String propertyName = entry.getKey();
                JsonNode propertyValue = entry.getValue();

                ValidationRules propertyRules = properties.get(propertyName);
                if (propertyRules != null) {
                    try {
                        propertyRules.validate(propertyValue);
                    } catch (ValidationException e) {
                        errors.add("Property '" + propertyName + "' validation failed: " + e.getMessage());
                    }
                } else if (Boolean.FALSE.equals(additionalProperties)) {
                    errors.add("Additional property not allowed: " + propertyName);
                }
            });
        }

        // Pattern property validations
        if (patternProperties != null) {
            value.fields().forEachRemaining(entry -> {
                String propertyName = entry.getKey();
                JsonNode propertyValue = entry.getValue();

                patternProperties.forEach((pattern, rules) -> {
                    if (propertyName.matches(pattern)) {
                        try {
                            rules.validate(propertyValue);
                        } catch (ValidationException e) {
                            errors.add("Pattern property '" + propertyName + "' validation failed: " + e.getMessage());
                        }
                    }
                });
            });
        }
    }

    private void validateEnum(JsonNode value, List<String> errors) {
        String stringValue = value.isTextual() ? value.asText() : value.toString();
        if (!enumValues.contains(stringValue)) {
            errors.add("Value must be one of: " + String.join(", ", enumValues));
        }
    }

    private boolean validateDateTime(String value) {
        try {
            OffsetDateTime.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
     

    private boolean validateTime(String value) {
        try {
            LocalTime.parse(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateEmail(String value) {
        return value.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private boolean validateUri(String value) {
        try {
            new URI(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean validateIpv4(String value) {
        return value.matches("^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");
    }

    private boolean validateIpv6(String value) {
        return value.matches("^(?:[A-F0-9]{1,4}:){7}[A-F0-9]{1,4}$");
    }

    private JsonSchemaValidator initializeValidator() {
        ObjectNode schemaNode = JsonNodeFactory.instance.objectNode();
        populateSchemaNode(schemaNode);
        
        // Convert ObjectNode to JSON string
        String schemaJson = schemaNode.toString();
        
        // Use the correct constructor or method
        return new JsonSchemaValidator(schemaJson);
    }

    private void populateSchemaNode(ObjectNode schemaNode) {
        if (type != null) schemaNode.put("type", type.toLowerCase());
        if (format != null) schemaNode.put("format", format);
        if (pattern != null) schemaNode.put("pattern", pattern);
        if (required != null) schemaNode.put("required", required);
        if (minimum != null) schemaNode.put("minimum", new BigDecimal(minimum));
        if (maximum != null) schemaNode.put("maximum", new BigDecimal(maximum));
        if (minLength != null) schemaNode.put("minLength", minLength);
        if (maxLength != null) schemaNode.put("maxLength", maxLength);
        if (minItems != null) schemaNode.put("minItems", minItems);
        if (maxItems != null) schemaNode.put("maxItems", maxItems);
        if (uniqueItems != null) schemaNode.put("uniqueItems", uniqueItems);
        if (enumValues != null) {
            ArrayNode enumNode = schemaNode.putArray("enum");
            enumValues.forEach(enumNode::add);
        }
    }
}
