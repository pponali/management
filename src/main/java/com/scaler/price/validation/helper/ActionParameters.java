package com.scaler.price.validation.helper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class ActionParameters {
    // Common Parameters
    private String value;             // The primary value for the action (price/discount/margin)
    private String discountType;      // PERCENTAGE, FIXED_AMOUNT
    private String roundingMode;      // UP, DOWN, HALF_UP, HALF_DOWN
    private Integer roundingDecimal;  // Number of decimal places

    // Set Price Parameters
    private Boolean allowPriceIncrease;    // Allow price to increase
    private BigDecimal maxPriceIncrease;   // Maximum allowed price increase
    private String priceSource;            // BASE_PRICE, COST_PRICE, MRP

    // Discount Parameters
    private BigDecimal discountAmount;     // New property for direct discount amount
    private BigDecimal maxDiscountAmount;  // Maximum discount amount
    private BigDecimal minDiscountAmount;  // Minimum discount amount
    private BigDecimal maxDiscountPercent; // Maximum discount percentage
    private Boolean stackable;             // Can be combined with other discounts
    private String discountPriority;       // Priority for stackable discounts

    // Margin Parameters
    private BigDecimal minimumMargin;      // Minimum margin percentage
    private BigDecimal maximumMargin;      // Maximum margin percentage
    private String marginCalculationType;   // ON_COST, ON_SALE
    private Boolean includeDiscounts;       // Include discounts in margin calculation

    // Competitor Price Parameters
    private String competitor;             // Competitor identifier
    private Set<String> competitors;       // Multiple competitors for comparison
    private String competitorPriceType;    // LOWEST, HIGHEST, AVERAGE
    private BigDecimal beatPercentage;     // Percentage to beat competitor
    private Boolean matchExactPrice;       // Match exact competitor price
    private Integer priceValidityHours;    // Competitor price validity

    // Bundle Parameters
    private String bundleId;               // Bundle identifier
    private Set<String> bundleIds;         // Multiple bundle identifiers
    private Integer minBundleQuantity;     // Minimum quantity for bundle
    private String bundleDiscountType;     // PERCENTAGE, FIXED_AMOUNT
    private BigDecimal bundleDiscountPercentage;     // Percentage discount for bundle
    private Boolean applyToAll;            // Apply to all items in bundle
    private Map<String, BigDecimal> itemDiscounts; // Item-specific discounts

    // Quantity Discount Parameters
    private String minQuantity;            // Minimum quantity
    private String maxQuantity;            // Maximum quantity
    private Integer minimumQuantity;      // Minimum quantity for discount
    private BigDecimal baseDiscount;      // Base discount percentage
    private BigDecimal tierIncrement;     // Discount increment per tier
    private Integer tierSize;              // Size of each tier
    private Map<Integer, BigDecimal> quantityTiers; // Quantity-discount mapping
    private Boolean applyToIncremental;    // Apply to incremental units only

    // Time-based Parameters
    private String timeSlot;               // Specific time slot
    private String dayOfWeek;              // Day of week
    private String timeZone;               // Time zone for time-based pricing
    private Integer validityHours;         // Validity period in hours

    // Custom Action Parameters
    private String actionHandler;          // Custom action handler identifier
    private Map<String, Object> customParameters; // Custom parameters

    // Channel Parameters
    private Set<String> channels;          // Applicable channels
    private Map<String, BigDecimal> channelAdjustments; // Channel-specific adjustments

    // Rounding Parameters
    private String roundingTarget;         // Target price point (e.g., .99, .95)
    private BigDecimal roundingStep;       // Step size for rounding
    private Boolean roundDown;             // Always round down

    // Validation Parameters
    private Boolean skipMarginValidation;  // Skip margin validation
    private Boolean skipMinPriceValidation; // Skip minimum price validation
    private Boolean skipMaxPriceValidation; // Skip maximum price validation
    private Boolean skipDiscountValidation; // Skip discount validation
    private BigDecimal discountPercentage;    // Discount percentage for validation

    @SuppressWarnings("unchecked")
    public ActionParameters(Map<String, Object> parameters) {
        if (parameters == null) {
            return;
        }

        this.value = (String) parameters.get("value");
        this.discountType = (String) parameters.get("discountType");
        this.roundingMode = (String) parameters.get("roundingMode");
        this.roundingDecimal = (Integer) parameters.get("roundingDecimal");
        this.allowPriceIncrease = (Boolean) parameters.get("allowPriceIncrease");
        
        if (parameters.get("maxPriceIncrease") != null) {
            this.maxPriceIncrease = new BigDecimal(parameters.get("maxPriceIncrease").toString());
        }
        
        this.priceSource = (String) parameters.get("priceSource");
        
        if (parameters.get("maxDiscountAmount") != null) {
            this.maxDiscountAmount = new BigDecimal(parameters.get("maxDiscountAmount").toString());
        }
        
        if (parameters.get("minDiscountAmount") != null) {
            this.minDiscountAmount = new BigDecimal(parameters.get("minDiscountAmount").toString());
        }
        
        if (parameters.get("maxDiscountPercent") != null) {
            this.maxDiscountPercent = new BigDecimal(parameters.get("maxDiscountPercent").toString());
        }
        
        if (parameters.get("discountAmount") != null) {
            this.discountAmount = new BigDecimal(parameters.get("discountAmount").toString());
        }
        
        this.stackable = (Boolean) parameters.get("stackable");
        this.discountPriority = (String) parameters.get("discountPriority");
        
        // Handle complex objects
        if (parameters.get("itemDiscounts") != null) {
            this.itemDiscounts = new HashMap<>();
            Map<String, Object> itemDiscountsMap = (Map<String, Object>) parameters.get("itemDiscounts");
            itemDiscountsMap.forEach((key, value) -> 
                this.itemDiscounts.put(key, new BigDecimal(value.toString())));
        }
        
        if (parameters.get("customParameters") != null) {
            this.customParameters = (Map<String, Object>) parameters.get("customParameters");
        }
    }

    public String getValue() {
        return this.value;
    }

    public Map<String, BigDecimal> getSeasonalFactors() {
        if (customParameters == null || !customParameters.containsKey("seasonalFactors")) {
            return new HashMap<>();
        }
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> factors = (Map<String, Object>) customParameters.get("seasonalFactors");
            Map<String, BigDecimal> result = new HashMap<>();
            
            factors.forEach((key, value) -> 
                result.put(key, new BigDecimal(value.toString())));
                
            return result;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static ActionParameters createSetPriceParams(BigDecimal price) {
        return ActionParameters.builder()
                .value(price.toString())
                .roundingMode("HALF_UP")
                .roundingDecimal(2)
                .allowPriceIncrease(false)
                .build();
    }

    public static ActionParameters createDiscountParams(
            BigDecimal discount,
            String discountType) {
        return ActionParameters.builder()
                .value(discount.toString())
                .discountType(discountType)
                .maxDiscountAmount(BigDecimal.valueOf(1000))
                .stackable(true)
                .discountPriority("HIGH")
                .build();
    }

    public static ActionParameters createMarginParams(BigDecimal margin) {
        return ActionParameters.builder()
                .value(margin.toString())
                .marginCalculationType("ON_COST")
                .minimumMargin(BigDecimal.valueOf(10))
                .maximumMargin(BigDecimal.valueOf(50))
                .includeDiscounts(true)
                .build();
    }

    public static ActionParameters createCompetitorParams(
            String competitor,
            BigDecimal beatBy) {
        return ActionParameters.builder()
                .competitor(competitor)
                .beatPercentage(beatBy)
                .minimumMargin(BigDecimal.valueOf(5))
                .matchExactPrice(false)
                .priceValidityHours(24)
                .build();
    }

    public static ActionParameters createBundleParams(
            String bundleId,
            BigDecimal discount) {
        return ActionParameters.builder()
                .bundleId(bundleId)
                .value(discount.toString())
                .bundleDiscountType("PERCENTAGE")
                .minBundleQuantity(2)
                .applyToAll(true)
                .build();
    }

    public static ActionParameters createQuantityDiscountParams(
            Integer minQty,
            BigDecimal discount) {
        return ActionParameters.builder()
                .minQuantity(minQty.toString())
                .value(discount.toString())
                .discountType("PERCENTAGE")
                .tierIncrement(BigDecimal.valueOf(5))
                .tierSize(5)
                .applyToIncremental(true)
                .build();
    }

    public void validateRequired() {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Value is required");
        }
    }

    public void validateDiscountParameters() {
        if (discountType == null ||
                (!discountType.equals("PERCENTAGE") && !discountType.equals("FIXED_AMOUNT"))) {
            throw new IllegalArgumentException("Invalid discount type");
        }
    }

    public void validateMarginParameters() {
        if (minimumMargin != null && maximumMargin != null &&
                minimumMargin.compareTo(maximumMargin) > 0) {
            throw new IllegalArgumentException("Minimum margin cannot be greater than maximum margin");
        }
    }

    public void validateCompetitorParameters() {
        if (competitor == null && competitors == null) {
            throw new IllegalArgumentException("Either competitor or competitors must be specified");
        }
    }

    public void validateBundleParameters() {
        if (bundleId == null && bundleIds == null) {
            throw new IllegalArgumentException("Bundle identifier is required");
        }
    }

    public void validateQuantityParameters() {
        if (minQuantity == null) {
            throw new IllegalArgumentException("Minimum quantity is required");
        }
    }

    public String getCompetitorId() {
        return this.competitor != null ? this.competitor : 
               (this.competitors != null && !this.competitors.isEmpty() ? this.competitors.iterator().next() : null);
    }
}
