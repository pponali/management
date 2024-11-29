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
                .validationRules(buildValidationRules())
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
                                .requiredProperties(List.of("adjustment"))
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
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "name", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(50)
                                .build(),
                        "startDate", ValidationRules.builder()
                                .type("STRING")
                                .format("date")
                                .build(),
                        "endDate", ValidationRules.builder()
                                .type("STRING")
                                .format("date")
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "priority", ValidationRules.builder()
                                .type("INTEGER")
                                .minimum("1")
                                .maximum("10")
                                .build()
                ))
                .requiredProperties(List.of("name", "startDate", "endDate", "adjustment"))
                .build();
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

    private ValidationRules buildDetailedDemandFactorsSchema() {
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
                                        "currentDemand", ValidationRules.builder()
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
                        "strategy", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("AGGRESSIVE", "CONSERVATIVE", "BALANCED"))
                                .build()
                ))
                .required(true)
                .requiredProperties(List.of("weight", "metrics", "strategy"))
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

    private ValidationRules buildSeasonalitySchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "peakSeasons", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildSeasonSchema())
                                .build(),
                        "offPeakAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "shoulderAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build()
                ))
                .build();
    }

    private ValidationRules buildServiceAreaSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "deliveryZones", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildDeliveryZoneSchema())
                                .build(),
                        "maxRadius", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build(),
                        "restrictions", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildServiceRestrictionSchema())
                                .build()
                ))
                .build();
    }

    private ValidationRules buildDeliveryZoneSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "zoneId", ValidationRules.builder()
                                .type("STRING")
                                .pattern("^[A-Z0-9_]{2,30}$")
                                .build(),
                        "radiusKm", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build(),
                        "deliveryFee", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build(),
                        "minOrderValue", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build()
                ))
                .requiredProperties(List.of("zoneId", "radiusKm", "deliveryFee"))
                .build();
    }

    private ValidationRules buildHolidaySchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "name", ValidationRules.builder()
                                .type("STRING")
                                .minLength(1)
                                .maxLength(50)
                                .build(),
                        "date", ValidationRules.builder()
                                .type("STRING")
                                .format("date")
                                .build(),
                        "isRecurring", ValidationRules.builder()
                                .type("BOOLEAN")
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build()
                ))
                .requiredProperties(List.of("name", "date"))
                .build();
    }

    private ValidationRules buildServiceRestrictionSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "type", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("WEATHER", "TRAFFIC", "EVENT", "MAINTENANCE"))
                                .build(),
                        "condition", ValidationRules.builder()
                                .type("STRING")
                                .maxLength(200)
                                .build(),
                        "impact", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("NO_SERVICE", "LIMITED_SERVICE", "DELAYED_SERVICE"))
                                .build(),
                        "adjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-100")
                                .maximum("100")
                                .build()
                ))
                .requiredProperties(List.of("type", "impact"))
                .build();
    }

    private ValidationRules buildGeographicFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "accessibility", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "footTraffic", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "parkingAvailability", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "publicTransport", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "busStops", ValidationRules.builder()
                                                .type("INTEGER")
                                                .minimum("0")
                                                .build(),
                                        "trainStations", ValidationRules.builder()
                                                .type("INTEGER")
                                                .minimum("0")
                                                .build(),
                                        "distance", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .build()
                                ))
                                .build()
                ))
                .requiredProperties(List.of("accessibility", "footTraffic"))
                .build();
    }

    private ValidationRules buildZoneAdjustmentFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "baseAdjustment", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("-50")
                                .maximum("50")
                                .build(),
                        "demandMultiplier", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0.1")
                                .maximum("10")
                                .build(),
                        "seasonalityImpact", ValidationRules.builder()
                                .type("BOOLEAN")
                                .build(),
                        "competitionResponse", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("AGGRESSIVE", "MODERATE", "PASSIVE"))
                                .build()
                ))
                .requiredProperties(List.of("baseAdjustment"))
                .build();
    }

    private ValidationRules buildDemandFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "historicalDemand", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "forecastDemand", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build(),
                        "peakHourMultiplier", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("1")
                                .maximum("5")
                                .build(),
                        "eventImpact", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "type", ValidationRules.builder()
                                                .type("STRING")
                                                .enumValues(List.of("SPORTS", "CONCERT", "FESTIVAL", "CONFERENCE"))
                                                .build(),
                                        "multiplier", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("1")
                                                .maximum("5")
                                                .build()
                                ))
                                .build()
                ))
                .requiredProperties(List.of("historicalDemand", "forecastDemand"))
                .build();
    }

    private ValidationRules buildWeatherFactorsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "temperature", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "min", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build(),
                                        "max", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build(),
                                        "optimal", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build()
                                ))
                                .build(),
                        "conditions", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(ValidationRules.builder()
                                        .type("STRING")
                                        .enumValues(List.of("SUNNY", "RAINY", "CLOUDY", "SNOWY", "STORMY"))
                                        .build())
                                .build(),
                        "adjustments", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "rain", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build(),
                                        "snow", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-50")
                                                .maximum("50")
                                                .build(),
                                        "extreme", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("-100")
                                                .maximum("100")
                                                .build()
                                ))
                                .build()
                ))
                .build();
    }

    private ValidationRules buildMarketSegmentSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "segmentType", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("PREMIUM", "STANDARD", "BUDGET", "CUSTOM"))
                                .build(),
                        "priceElasticity", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("10")
                                .build(),
                        "customerProfile", buildCustomerProfileSchema(),
                        "spendingPatterns", buildSpendingPatternsSchema()
                ))
                .requiredProperties(List.of("segmentType", "priceElasticity"))
                .build();
    }

    private ValidationRules buildCustomerProfileSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "ageGroup", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("YOUTH", "ADULT", "SENIOR", "ALL"))
                                .build(),
                        "incomeLevel", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("LOW", "MEDIUM", "HIGH"))
                                .build(),
                        "purchaseFrequency", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("DAILY", "WEEKLY", "MONTHLY", "OCCASIONAL"))
                                .build(),
                        "loyaltyScore", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .maximum("100")
                                .build()
                ))
                .requiredProperties(List.of("ageGroup", "incomeLevel"))
                .build();
    }

    private ValidationRules buildSpendingPatternsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "averageOrderValue", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build(),
                        "peakSpendingTime", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildTimeWindowSchema())
                                .build(),
                        "seasonalTrends", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(buildSeasonalTrendSchema())
                                .build(),
                        "priceThresholds", buildPriceThresholdsSchema()
                ))
                .requiredProperties(List.of("averageOrderValue"))
                .build();
    }

    private ValidationRules buildSeasonalTrendSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "season", ValidationRules.builder()
                                .type("STRING")
                                .enumValues(List.of("SPRING", "SUMMER", "AUTUMN", "WINTER"))
                                .build(),
                        "spendingMultiplier", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0.1")
                                .maximum("10")
                                .build(),
                        "popularCategories", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(ValidationRules.builder()
                                        .type("STRING")
                                        .maxLength(50)
                                        .build())
                                .build()
                ))
                .requiredProperties(List.of("season", "spendingMultiplier"))
                .build();
    }

    private ValidationRules buildPriceThresholdsSchema() {
        return ValidationRules.builder()
                .type("OBJECT")
                .properties(Map.of(
                        "psychological", ValidationRules.builder()
                                .type("ARRAY")
                                .itemSchema(ValidationRules.builder()
                                        .type("NUMBER")
                                        .minimum("0")
                                        .build())
                                .build(),
                        "resistance", ValidationRules.builder()
                                .type("NUMBER")
                                .minimum("0")
                                .build(),
                        "sensitivity", ValidationRules.builder()
                                .type("OBJECT")
                                .properties(Map.of(
                                        "low", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .build(),
                                        "high", ValidationRules.builder()
                                                .type("NUMBER")
                                                .minimum("0")
                                                .build()
                                ))
                                .build()
                ))
                .requiredProperties(List.of("resistance"))
                .build();
    }
}
