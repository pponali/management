package com.scaler.price.rule.schema;

import com.scaler.price.rule.dto.ParameterDefinition;
import com.scaler.price.rule.dto.ParameterSchema;
import com.scaler.price.rule.validation.ValidationRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class SeasonalPricingSchemaBuilder {

    public ParameterSchema buildSeasonalPricingSchema() {
        return ParameterSchema.builder()
                .parameters(buildParameterDefinitions())
                .requiredParameters(buildRequiredParameters())
                .dependencyRules(buildDependencyRules())
                .validationRules(buildValidationRules())
                .build();
    }

    private Map<String, ParameterDefinition> buildParameterDefinitions() {
        Map<String, ParameterDefinition> parameters = new HashMap<>();

        // Base Season Configuration
        parameters.put("seasons", ParameterDefinition.builder()
                .name("seasons")
                .type("ARRAY")
                .description("List of seasonal configurations")
                .defaultValue(Collections.emptyList())
                .validationRules(ValidationRules.builder()
                        .minItems(1)
                        .maxItems(4)
                        .uniqueItems(true)
                        .itemSchema(buildSeasonSchema())
                        .build())
                .build());

        // Price Adjustment Configuration
        parameters.put("adjustmentType", ParameterDefinition.builder()
                .name("adjustmentType")
                .type("STRING")
                .description("Type of price adjustment")
                .defaultValue("PERCENTAGE")
                .validationRules(ValidationRules.builder()
                        .enumValues(List.of("PERCENTAGE", "FIXED_AMOUNT", "MULTIPLIER"))
                        .build())
                .build());

        // Default Adjustment
        parameters.put("defaultAdjustment", ParameterDefinition.builder()
                .name("defaultAdjustment")
                .type("NUMBER")
                .description("Default adjustment when no season matches")
                .defaultValue(BigDecimal.ZERO)
                .validationRules(ValidationRules.builder()
                        .minimum("-100")
                        .maximum("100")
                        .build())
                .build());

        // Price Bounds
        parameters.put("priceBounds", ParameterDefinition.builder()
                .name("priceBounds")
                .type("OBJECT")
                .description("Price boundaries for seasonal adjustments")
                .validationRules(ValidationRules.builder()
                        .required(true)
                        .properties(buildPriceBoundsSchema())
                        .build())
                .build());

        // Priority Configuration
        parameters.put("priority", ParameterDefinition.builder()
                .name("priority")
                .type("NUMBER")
                .description("Priority of seasonal pricing rule")
                .defaultValue(1)
                .validationRules(ValidationRules.builder()
                        .minimum("1")
                        .maximum("100")
                        .build())
                .build());

        // Time Zone Configuration
        parameters.put("timezone", ParameterDefinition.builder()
                .name("timezone")
                .type("STRING")
                .description("Timezone for seasonal calculations")
                .defaultValue("Asia/Kolkata")
                .validationRules(ValidationRules.builder()
                        .pattern("^[A-Za-z]+/[A-Za-z_]+$")
                        .build())
                .build());

        // Overlap Handling
        parameters.put("overlapStrategy", ParameterDefinition.builder()
                .name("overlapStrategy")
                .type("STRING")
                .description("How to handle overlapping seasons")
                .defaultValue("HIGHEST_ADJUSTMENT")
                .validationRules(ValidationRules.builder()
                        .enumValues(List.of(
                                "HIGHEST_ADJUSTMENT",
                                "LOWEST_ADJUSTMENT",
                                "COMBINE_ADJUSTMENTS"
                        ))
                        .build())
                .build());

        return parameters;
    }

    private ValidationRules buildSeasonSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .required(true)
                .properties(Map.of(
                        "seasonId", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^[A-Z_]{2,30}$")
                                .build(),
                        "name", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(50)
                                .build(),
                        "startDate", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])$")
                                .build(),
                        "endDate", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^(0[1-9]|[12][0-9]|3[01])-(0[1-9]|1[012])$")
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-100")
                                .maximum("100")
                                .build(),
                        "conditions", buildSeasonConditionsSchema()
                ))
                .build();
    }

    private ValidationRules buildSeasonConditionsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "categories", ValidationRules.builder()
                                .type("ARRAY")
                                .itemType("STRING")
                                .build(),
                        "brands", ValidationRules.builder()
                                .type("ARRAY")
                                .itemType("STRING")
                                .build(),
                        "locations", ValidationRules.builder()
                                .type("ARRAY")
                                .itemType("STRING")
                                .build(),
                        "timeRanges", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildTimeRangeSchema())
                                .build(),
                        "customAttributes", ValidationRules.builder()
                                .type("OBJECT")
                                .additionalProperties(true)
                                .build()
                ))
                .build();
    }

    private ValidationRules buildTimeRangeSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .required(true)
                .properties(Map.of(
                        "startTime", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                                .build(),
                        "endTime", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                                .build(),
                        "days", ValidationRules.builder()
                                .type("ARRAY")
                                .itemType("STRING")
                                .enumValues(List.of(
                                        "MONDAY", "TUESDAY", "WEDNESDAY",
                                        "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
                                ))
                                .build()
                ))
                .build();
    }

    private Map<String, ValidationRules> buildPriceBoundsSchema() {
        return Map.of(
                "minimumPrice", ValidationRules.builder()
                        .type("NUMBER")
                        .minimum("0")
                        .build(),
                "maximumPrice", ValidationRules.builder()
                        .type("NUMBER")
                        .minimum("0")
                        .build(),
                "minimumMargin", ValidationRules.builder()
                        .type("NUMBER")
                        .minimum("0")
                        .maximum("100")
                        .build(),
                "roundingMethod", ValidationRules.builder()
                        .type("STRING")
                        .enumValues(List.of(
                                "NONE", "CEIL", "FLOOR",
                                "NEAREST_5", "NEAREST_10", "NEAREST_100"
                        ))
                        .build()
        );
    }

    private List<String> buildRequiredParameters() {
        return Arrays.asList(
                "seasons",
                "adjustmentType",
                "priceBounds"
        );
    }

    private Map<String, List<String>> buildDependencyRules() {
        Map<String, List<String>> dependencies = new HashMap<>();

        // Adjustment type dependencies
        dependencies.put("adjustmentType", List.of("defaultAdjustment"));

        // Price bounds dependencies
        dependencies.put("priceBounds.minimumPrice",
                List.of("priceBounds.maximumPrice"));
        dependencies.put("priceBounds.roundingMethod",
                List.of("adjustmentType"));

        return dependencies;
    }

    private Map<String, String> buildValidationRules() {
        return Map.of(
                "PRICE_BOUNDS", "minimumPrice must be less than maximumPrice",
                "SEASON_DATES", "endDate must be after startDate",
                "TIME_RANGE", "endTime must be after startTime",
                "ADJUSTMENT_RANGE", "adjustment must be within valid range for adjustmentType"
        );
    }
}
