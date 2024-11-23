package com.scaler.price.rule.schema;

import com.scaler.price.rule.dto.ParameterDefinition;
import com.scaler.price.rule.dto.ParameterSchema;
import com.scaler.price.rule.validation.ValidationRules;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class LocationPricingSchemaBuilder {

    public ParameterSchema buildLocationPricingSchema() {
        return ParameterSchema.builder()
                .parameters(buildParameterDefinitions())
                .requiredParameters(buildRequiredParameters())
                .dependencyRules(buildDependencyRules())
                .dependencyRules(buildValidationRules())
                .build();
    }

    private Map<String, ParameterDefinition> buildParameterDefinitions() {
        return null;
    }

    private ValidationRules buildZoneMappingSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .patternProperties(Map.of(
                        "^[A-Z_]+$", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "adjustment", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build(),
                                        "priority", ValidationRules.builder()
                                                .type("INTEGER")
                                                .minimum("1")
                                                .maximum("100")
                                                .build(),
                                        "active", ValidationRules.builder()
                                                .type("BOOLEAN")
                                                .build()
                                ))
                                .required(List.of("adjustment"))
                                .build()
                ))
                .build();
    }

    private Map<String, ValidationRules> buildLocationFactorsSchema() {
        return Map.of(
                "demand", buildDemandFactorSchema(),
                "competition", buildCompetitionFactorSchema(),
                "purchasingPower", buildPurchasingPowerFactorSchema(),
                "accessibility", buildAccessibilityFactorSchema()
        );
    }

    private ValidationRules buildDemandFactorSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "thresholds", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildThresholdSchema())
                                .build(),
                        "aggregationType", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("DAILY", "WEEKLY", "MONTHLY"))
                                .build()
                ))
                .build();
    }

    private ValidationRules buildCompetitionFactorSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "competitors", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildCompetitorSchema())
                                .build(),
                        "strategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of(
                                        "MATCH_LOWEST",
                                        "BEAT_AVERAGE",
                                        "MAINTAIN_POSITION"
                                ))
                                .build()
                ))
                .build();
    }

    private ValidationRules buildGeographicRuleSchema() {
        return null;
    }

    private ValidationRules buildGeographicConditionsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "minPopulation", ValidationRules.builder()
                                .type("INTEGER")
                                .minimum("0")
                                .build(),
                        "maxPopulation", ValidationRules.builder()
                                .type("INTEGER")
                                .minimum("0")
                                .build(),
                        "tier", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("TIER_1", "TIER_2", "TIER_3"))
                                .build(),
                        "attributes", ValidationRules.builder()
                                .type("OBJECT")
                                .additionalProperties(true)
                                .build()
                ))
                .build();
    }

    

    private List<String> buildRequiredParameters() {
        return Arrays.asList(
                "baseAdjustment",
                "zoneMappings",
                "priceBounds"
        );
    }

    private Map<String, List<String>> buildDependencyRules() {
        Map<String, List<String>> dependencies = new HashMap<>();

        dependencies.put("competitionFactors",
                List.of("locationFactors"));
        dependencies.put("seasonalAdjustments",
                List.of("timeRestrictions"));

        return dependencies;
    }

    private Map<String, String> buildValidationRules() {
        return Map.of(
                "ZONE_MAPPING", "Zone mappings must be unique and valid",
                "PRICE_BOUNDS", "Price bounds must be valid and consistent",
                "GEOGRAPHIC_RULES", "Geographic rules must not conflict",
                "TIME_RESTRICTIONS", "Time restrictions must not overlap"
        );
    }

    private ValidationRules buildBlackoutDateSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "date", ValidationRules.builder()
                                .type("STRING")
                                .format("date")
                                .build(),
                        "reason", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(200)
                                .build(),
                        "impact", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("DISABLE_PRICING", "USE_DEFAULT", "CUSTOM_ADJUSTMENT"))
                                .build(),
                        "customAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-100")
                                .maximum("100")
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("date", "impact"))
                .build();
    }

    private ValidationRules buildTimeWindowSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "days", ValidationRules.builder()
                                .type("ARRAY")
                                .itemType("STRING")
                                .enumValues(List.of("MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"))
                                .minItems(1)
                                .uniqueItems(true)
                                .build(),
                        "startTime", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                                .build(),
                        "endTime", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "priority", ValidationRules.builder()
                                .type("INTEGER")
                                .minimum("1")
                                .maximum("100")
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("days", "startTime", "endTime", "adjustment"))
                .build();
    }

    private ValidationRules buildCompetitorSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "competitorId", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^[A-Z0-9_]{2,30}$")
                                .build(),
                        "name", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(100)
                                .build(),
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "maxDifference", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "strategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("MATCH", "BEAT", "LAG", "CUSTOM"))
                                .build(),
                        "customStrategy", ValidationRules.builder()
                                .type("OBJECT")
                                .additionalProperties(true)
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("competitorId", "weight", "strategy"))
                .build();
    }

    private ValidationRules buildThresholdSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "level", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("LOW", "MEDIUM", "HIGH", "CRITICAL"))
                                .build(),
                        "minScore", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "maxScore", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "conditions", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildThresholdConditionSchema())
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("level", "minScore", "adjustment"))
                .build();
    }

    private ValidationRules buildAccessibilityFactorSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "factors", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "publicTransport", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "parking", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "walkability", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "trafficDensity", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build()
                                ))
                                .build(),
                        "thresholds", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildThresholdSchema())
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("weight", "factors"))
                .build();
    }

    private ValidationRules buildPurchasingPowerFactorSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "metrics", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "averageIncome", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .build(),
                                        "spendingCapacity", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("100")
                                                .build(),
                                        "disposableIncome", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .build()
                                ))
                                .build(),
                        "thresholds", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildThresholdSchema())
                                .build(),
                        "adjustmentStrategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("LINEAR", "EXPONENTIAL", "STEPPED"))
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("weight", "metrics"))
                .build();
    }

    private ValidationRules buildTimeRestrictionsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "effectiveFrom", ValidationRules.builder()
                                .type("STRING")
                                .format("date-time")
                                .build(),
                        "effectiveTo", ValidationRules.builder()
                                .type("STRING")
                                .format("date-time")
                                .build(),
                        "blackoutDates", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildBlackoutDateSchema())
                                .uniqueItems(true)
                                .build(),
                        "timeWindows", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildTimeWindowSchema())
                                .build(),
                        "holidayCalendar", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("DEFAULT", "CUSTOM"))
                                .build(),
                        "customCalendar", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildCustomCalendarSchema())
                                .build()
                ))
                .build();
    }

    private ValidationRules buildPriceBoundsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
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
                        "maximumMargin", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "roundingMethod", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("NONE", "CEIL", "FLOOR", "NEAREST_5", "NEAREST_10", "NEAREST_100"))
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("minimumPrice", "maximumPrice"))
                .build();
    }

    private ValidationRules buildSeasonalAdjustmentsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "seasons", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildSeasonSchema())
                                .minItems(1)
                                .maxItems(4)
                                .uniqueItems(true)
                                .build(),
                        "defaultAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "overrideStrategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("HIGHEST", "LOWEST", "AVERAGE"))
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("seasons"))
                .build();
    }

    private ValidationRules buildSeasonSchema() {
    }


    private ValidationRules buildCompetitionFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "competitors", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildCompetitorSchema())
                                .minItems(1)
                                .uniqueItems(true)
                                .build(),
                        "strategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("MATCH_LOWEST", "BEAT_AVERAGE", "MAINTAIN_POSITION"))
                                .build(),
                        "maxAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "updateFrequency", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("REAL_TIME", "HOURLY", "DAILY"))
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("weight", "competitors", "strategy"))
                .build();
    }

    private ValidationRules buildDemandFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "weight", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build(),
                        "metrics", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "historicalSales", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "searchVolume", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "cartAbandonment", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build(),
                                        "stockLevel", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .maximum("1")
                                                .build()
                                ))
                                .build(),
                        "thresholds", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildThresholdSchema())
                                .build(),
                        "aggregationType", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("DAILY", "WEEKLY", "MONTHLY"))
                                .build(),
                        "smoothingFactor", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("1")
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("weight", "metrics", "thresholds"))
                .build();
    }

    private ValidationRules buildCustomCalendarSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "date", ValidationRules.builder()
                                .type("STRING")
                                .format("date")
                                .build(),
                        "name", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(100)
                                .build(),
                        "type", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("HOLIDAY", "EVENT", "SALE", "OTHER"))
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("date", "type"))
                .build();
    }

    private ValidationRules buildThresholdConditionSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "metric", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(50)
                                .build(),
                        "operator", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("EQ", "GT", "LT", "GTE", "LTE", "BETWEEN"))
                                .build(),
                        "value", ValidationRules.builder()
                                .type("STRING")
                                .build(),
                        "secondValue", ValidationRules.builder()
                                .type("STRING")
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("metric", "operator", "value"))
                .build();
    }
}

// Example Configuration:
/*
{
    "baseAdjustment": 5.0,
    "zoneMappings": {
        "ZONE_A": {
            "adjustment": 10.0,
            "priority": 1,
            "active": true
        },
        "ZONE_B": {
            "adjustment": 5.0,
            "priority": 2,
            "active": true
        }
    },
    "locationFactors": {
        "demand": {
            "weight": 0.4,
            "thresholds": [
                {
                    "level": "HIGH",
                    "adjustment": 5.0,
                    "minScore": 80
                }
            ],
            "aggregationType": "DAILY"
        },
        "competition": {
            "weight": 0.3,
            "competitors": [
                {
                    "name": "COMPETITOR_A",
                    "weight": 0.6,
                    "maxDifference": 10.0
                }
            ],
            "strategy": "BEAT_AVERAGE"
        }
    },
    "geographicRules": [
        {
            "type": "CITY",
            "value": "MUMBAI",
            "adjustment": 8.0,
            "conditions": {
                "tier": "TIER_1",
                "attributes": {
                    "isMetro": true
                }
            }
        }
    ],
    "priceBounds": {
        "minimumPrice": 100.0,
        "maximumPrice": 10000.0,
        "minimumMargin": 15.0,
        "roundingMethod": "NEAREST_10"
    },
    "timeRestrictions": {
        "effectiveFrom": "2024-01-01T00:00:00Z",
        "effectiveTo": "2024-12-31T23:59:59Z",
        "timeWindows": [
            {
                "days": ["MONDAY", "TUESDAY"],
                "startTime": "09:00",
                "endTime": "18:00",
                "adjustment": 2.0
            }
        ]
    }
}
*/




