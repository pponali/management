package com.scaler.price.rule.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scaler.price.rule.config.ConfigurationService;
import com.scaler.price.rule.domain.*;
import com.scaler.price.rules.domain.*;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.rule.dto.TimeRestrictions;
import com.scaler.price.rule.exceptions.RuleValidationException;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.validation.services.ActionValidator;
import com.scaler.price.validation.services.ConditionValidator;
import com.scaler.price.validation.services.PriceValidator;
import io.micrometer.common.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.scaler.price.rule.domain.ConditionType.*;
import static com.scaler.price.validation.services.DiscountValidator.MAX_STACK_COUNT;

// RuleValidationService.java
@Service
@Slf4j

public class RuleValidationService {
    private final SellerService sellerService;
    private final SiteService siteService;
    private final CategoryService categoryService;
    private final BrandService brandService;
    private final ActionValidator actionValidator;
    private final ConditionValidator conditionValidator;
    private final PriceValidator priceValidator;
    private final ConfigurationService configService;
    private final RuleRepository ruleRepository;
    private final ObjectMapper objectMapper;

    public RuleValidationService(SellerService sellerService, SiteService siteService, CategoryService categoryService, BrandService brandService, ActionValidator actionValidator, ConditionValidator conditionValidator, PriceValidator priceValidator, ConfigurationService configService, RuleRepository ruleRepository, ObjectMapper objectMapper, CompetitorService competitorService) {
        this.sellerService = sellerService;
        this.siteService = siteService;
        this.categoryService = categoryService;
        this.brandService = brandService;
        this.actionValidator = actionValidator;
        this.conditionValidator = conditionValidator;
        this.priceValidator = priceValidator;
        this.configService = configService;
        this.ruleRepository = ruleRepository;
        this.objectMapper = objectMapper;
        this.competitorService = competitorService;
    }

    public void validateRule(PricingRule rule) throws RuleValidationException {
        log.debug("Validating rule: {}", rule.getRuleName());

        validateBasicFields(rule);
        validateDates(rule);
        priceValidator.validatePrices(rule);
        priceValidator.validateMargins(rule);
        validateMappings(rule);
        conditionValidator.validateConditions(rule.getConditions());
        actionValidator.validateActions(rule.getActions());
        validateRuleLimits(rule);
        validateConflicts(rule);
    }

    public void validateRuleUpdate(PricingRule existingRule, PricingRule updatedRule) throws RuleValidationException {
        validateRule(updatedRule);
        validateUpdateSpecificRules(existingRule, updatedRule);
    }

    private void validateBasicFields(PricingRule rule) throws RuleValidationException {
        log.debug("Validating basic fields for rule: {}", rule.getRuleName());

        if (StringUtils.isBlank(rule.getRuleName())) {
            throw new RuleValidationException("Rule name is required");
        }

        if (rule.getRuleName().length() > configService.getMaxRuleNameLength()) {
            throw new RuleValidationException("Rule name exceeds maximum length");
        }

        if (StringUtils.isBlank(rule.getDescription())) {
            throw new RuleValidationException("Rule description is required");
        }

        if (rule.getRuleType() == null) {
            throw new RuleValidationException("Rule type is required");
        }

        if (rule.getPriority() == null || rule.getPriority() < 0) {
            throw new RuleValidationException("Valid priority is required");
        }
    }

    private void validateDates(PricingRule rule) throws RuleValidationException {
        if (rule.getEffectiveFrom() == null) {
            throw new RuleValidationException("Effective from date is required");
        }

        LocalDateTime now = LocalDateTime.now();
        if (rule.getEffectiveFrom().isBefore(now.minusMinutes(5))) {
            throw new RuleValidationException("Effective from date cannot be in the past");
        }

        if (rule.getEffectiveTo() != null) {
            if (rule.getEffectiveTo().isBefore(rule.getEffectiveFrom())) {
                throw new RuleValidationException("Effective to date must be after effective from date");
            }

            long durationInDays = ChronoUnit.DAYS.between(
                    rule.getEffectiveFrom(),
                    rule.getEffectiveTo()
            );

            if (durationInDays > configService.getMaxRuleDurationDays()) {
                throw new RuleValidationException(
                        "Rule duration exceeds maximum allowed days: " +
                                configService.getMaxRuleDurationDays()
                );
            }
        }
    }

    private void validateMappings(PricingRule rule) throws RuleValidationException {
        validateSellerMappings(rule.getSellerIds());
        validateSiteMappings(rule.getSiteIds());
        validateCategoryMappings(rule.getCategoryIds());
        validateBrandMappings(rule.getBrandIds());
    }

    private void validateSellerMappings(Set<String> sellerIds) throws RuleValidationException {
        if (sellerIds == null || sellerIds.isEmpty()) {
            throw new RuleValidationException("At least one seller mapping is required");
        }

        for (String sellerId : sellerIds) {
            if (!sellerService.isValidSeller(sellerId)) {
                throw new RuleValidationException("Invalid seller ID: " + sellerId);
            }

            if (!sellerService.isSellerActive(sellerId)) {
                throw new RuleValidationException("Seller is not active: " + sellerId);
            }
        }
    }

    private void validateSiteMappings(Set<String> siteIds) throws RuleValidationException {
        if (siteIds == null || siteIds.isEmpty()) {
            throw new RuleValidationException("At least one site mapping is required");
        }

        for (String siteId : siteIds) {
            if (!siteService.isValidSite(siteId)) {
                throw new RuleValidationException("Invalid site ID: " + siteId);
            }

            if (!siteService.isSiteActive(siteId)) {
                throw new RuleValidationException("Site is not active: " + siteId);
            }
        }
    }

    private void validateCategoryMappings(Set<String> categoryIds) throws RuleValidationException {
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (String categoryId : categoryIds) {
                if (!categoryService.isValidCategory(categoryId)) {
                    throw new RuleValidationException("Invalid category ID: " + categoryId);
                }
            }
        }
    }

    private void validateBrandMappings(Set<String> brandIds) throws RuleValidationException {
        if (brandIds != null && !brandIds.isEmpty()) {
            for (String brandId : brandIds) {
                if (!brandService.isValidBrand(brandId)) {
                    throw new RuleValidationException("Invalid brand ID: " + brandId);
                }
            }
        }
    }

    private void validateRuleLimits(PricingRule rule) throws RuleValidationException {
        validateAgainstSystemLimits(rule);

        for (String sellerId : rule.getSellerIds()) {
            validateAgainstSellerLimits(rule, sellerId);
        }

        for (String siteId : rule.getSiteIds()) {
            validateAgainstSiteLimits(rule, siteId);
        }
    }

    private void validateAgainstSystemLimits(PricingRule rule) throws RuleValidationException {
        int maxRulesPerSeller = configService.getMaxRulesPerSeller();
        int maxRulesPerSite = configService.getMaxRulesPerSite();

        for (String sellerId : rule.getSellerIds()) {
            long sellerRuleCount = ruleRepository.countBySellerIdsContaining(sellerId);
            if (sellerRuleCount >= maxRulesPerSeller) {
                throw new RuleValidationException(
                        "Seller " + sellerId + " has reached maximum allowed rules"
                );
            }
        }

        for (String siteId : rule.getSiteIds()) {
            long siteRuleCount = ruleRepository.countBySiteIdsContaining(siteId);
            if (siteRuleCount >= maxRulesPerSite) {
                throw new RuleValidationException(
                        "Site " + siteId + " has reached maximum allowed rules"
                );
            }
        }
    }

    private void validateAgainstSellerLimits(PricingRule rule, String sellerId) throws RuleValidationException {
        SellerLimits limits = sellerService.getSellerLimits(sellerId);

        if (limits.getMaxRules() > 0) {
            long currentRuleCount = ruleRepository.countBySellerIdsContaining(sellerId);
            if (currentRuleCount >= limits.getMaxRules()) {
                throw new RuleValidationException(
                        "Seller has reached maximum allowed rules: " + limits.getMaxRules()
                );
            }
        }

        if (limits.getMaxDiscount() != null) {
            validateDiscountAction(rule, limits.getMaxDiscount());
        }
    }

    private void validateAgainstSiteLimits(PricingRule rule, String siteId) throws RuleValidationException {
        SiteLimits limits = siteService.getSiteLimits(siteId);

        if (limits.getMaxRules() > 0) {
            long currentRuleCount = ruleRepository.countBySiteIdsContaining(siteId);
            if (currentRuleCount >= limits.getMaxRules()) {
                throw new RuleValidationException(
                        "Site has reached maximum allowed rules: " + limits.getMaxRules()
                );
            }
        }

        if (limits.getMaxDiscount() != null) {
            validateDiscountAgainstLimit(rule, limits.getMaxDiscount());
        }
    }

    private void validateConflicts(PricingRule rule) throws RuleValidationException {
        List<PricingRule> conflictingRules = findConflictingRules(rule);

        if (!conflictingRules.isEmpty()) {
            throw new RuleValidationException(
                    "Rule conflicts with existing rules: " +
                            conflictingRules.stream()
                                    .map(PricingRule::getRuleName)
                                    .collect(Collectors.joining(", "))
            );
        }
    }

    private List<PricingRule> findConflictingRules(PricingRule rule) {
        return ruleRepository.findConflictingRules(
                rule.getSellerIds(),
                rule.getSiteIds(),
                rule.getEffectiveFrom(),
                rule.getEffectiveTo()
        );
    }



    private boolean hasSignificantChanges(PricingRule existingRule, PricingRule updatedRule) {
        return !existingRule.getConditions().equals(updatedRule.getConditions()) ||
                !existingRule.getActions().equals(updatedRule.getActions());
    }


    private void validatePrices(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumPrice() != null && rule.getMaximumPrice() != null) {
            if (rule.getMinimumPrice().compareTo(rule.getMaximumPrice()) > 0) {
                throw new RuleValidationException(
                        "Minimum price cannot be greater than maximum price"
                );
            }

            if (rule.getMinimumPrice().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuleValidationException("Minimum price cannot be negative");
            }
        }
    }

    private void validateMargins(PricingRule rule) throws RuleValidationException {
        if (rule.getMinimumMargin() != null && rule.getMaximumMargin() != null) {
            if (rule.getMinimumMargin().compareTo(rule.getMaximumMargin()) > 0) {
                throw new RuleValidationException(
                        "Minimum margin cannot be greater than maximum margin"
                );
            }

            if (rule.getMinimumMargin().compareTo(BigDecimal.ZERO) < 0 ||
                    rule.getMaximumMargin().compareTo(new BigDecimal("100")) > 0) {
                throw new RuleValidationException(
                        "Margins must be between 0 and 100 percent"
                );
            }
        }
    }

    @SneakyThrows
    private void validateConditions(Set<RuleCondition> conditions) throws RuleValidationException {
        if (conditions == null || conditions.isEmpty()) {
            throw new RuleValidationException("At least one condition is required");
        }

        if (conditions.size() > configService.getMaxConditionsPerRule()) {
            throw new RuleValidationException(
                    "Number of conditions exceeds maximum allowed: " +
                            configService.getMaxConditionsPerRule()
            );
        }

        conditions.forEach(this::validateCondition);
    }

    private void validateCondition(RuleCondition condition) throws RuleValidationException {
        if (condition.getType() == null) {
            throw new RuleValidationException("Condition type is required");
        }

        if (StringUtils.isBlank(condition.getAttribute())) {
            throw new RuleValidationException("Condition attribute is required");
        }

        if (condition.getOperator() == null) {
            throw new RuleValidationException("Condition operator is required");
        }

        validateConditionValue(condition);
    }

    private void validateConditionValue(RuleCondition condition) throws RuleValidationException {
        if (condition.getValue() == null) {
            throw new RuleValidationException("Condition value is required");
        }

        try {
            switch (condition) {
                case PRICE_RANGE:
                case MARGIN_RANGE:
                    validateNumericValue(condition.getValue());
                    break;
                case INVENTORY_LEVEL:
                    validateIntegerValue(condition.getValue());
                    break;
                case TIME_BASED:
                    validateTimeValue(condition.getValue());
                    break;
                case COMPETITOR_PRICE:
                    validateCompetitorValue(condition.getValue());
                    break;
            }
        } catch (Exception e) {
            throw new RuleValidationException(
                    "Invalid condition value for type " + condition.getType() +
                            ": " + e.getMessage()
            );
        }
    }

    @SneakyThrows
    private void validateActions(List<RuleAction> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("At least one action is required");
        }

        if (actions.size() > configService.getMaxActionsPerRule()) {
            throw new RuleValidationException(
                    "Number of actions exceeds maximum allowed: " +
                            configService.getMaxActionsPerRule()
            );
        }

        validateActionSequences(actions);
        actions.forEach(this::validateAction);
    }

    private void validateAction(RuleAction action) throws RuleValidationException {
        if (action.getType() == null) {
            throw new RuleValidationException("Action type is required");
        }

        if (action.getSequence() == null || action.getSequence() < 0) {
            throw new RuleValidationException(
                    "Valid action sequence is required"
            );
        }

        switch (action.getType()) {
            case CUSTOM:
                validateCustomAction(action);
                break;
            case SET_PRICE:
                validateSetPriceAction(action);
                break;
            case APPLY_DISCOUNT:
                validateDiscountAction(action);
                break;
        }

        validateActionParameters(action);
    }

    private void validateActionParameters(RuleAction action) throws RuleValidationException {
        try {
            ActionParameters params = objectMapper.readValue(
                    action.getParameters(),
                    ActionParameters.class
            );

            params.validateRequired();

            switch (action.getType()) {
                case SET_PRICE:
                    validateSetPriceParameters(params);
                    break;
                case APPLY_DISCOUNT_PERCENTAGE:
                case APPLY_DISCOUNT_AMOUNT:
                    params.validateDiscountParameters();
                    break;
                case SET_MARGIN:
                    params.validateMarginParameters();
                    break;
                case MATCH_COMPETITOR_PRICE:
                case BEAT_COMPETITOR_PRICE:
                    params.validateCompetitorParameters();
                    break;
                case BUNDLE_DISCOUNT:
                    params.validateBundleParameters();
                    break;
                case QUANTITY_DISCOUNT:
                    params.validateQuantityParameters();
                    break;
            }
        } catch (Exception e) {
            throw new RuleValidationException(
                    "Invalid action parameters: " + e.getMessage()
            );
        }
    }



    private final CompetitorService competitorService;

    private static final BigDecimal MAX_PRICE_CHANGE_PERCENTAGE = new BigDecimal("50.0");
    private static final BigDecimal MIN_PRICE = new BigDecimal("0.01");
    private static final int MAX_ACTIONS_PER_RULE = 5;
    private static final Pattern TIME_PATTERN = Pattern.compile("^([01]?[0-9]|2[0-3]):[0-5][0-9]$");

    public void validateNonStackableDiscount(DiscountAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Discount action cannot be null");
        }
        if (action.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuleValidationException("Non-stackable discount value must be greater than zero");
        }
        validateDiscountValue(action.getDiscountValue(), action.getDiscountType());
    }

    public void validateDiscountValue(BigDecimal discountValue, String discountType) throws RuleValidationException {
        if (discountValue == null || discountValue.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuleValidationException("Discount value must be greater than zero");
        }
        if ("PERCENTAGE".equals(discountType) && discountValue.compareTo(new BigDecimal("100")) > 0) {
            throw new RuleValidationException("Percentage discount cannot exceed 100%");
        }
    }


    /**
     * Validates update-specific rules that apply only during rule updates
     * @param existingRule The existing rule to be updated
     * @param updatedRule The new rule data
     * @throws RuleValidationException if validation fails
     */
    public void validateUpdateSpecificRules(PricingRule existingRule, PricingRule updatedRule) throws RuleValidationException {
        if (existingRule == null) {
            throw new RuleValidationException("Existing rule cannot be null");
        }

        // Validate immutable fields haven't changed
        if (!existingRule.getRuleType().equals(updatedRule.getRuleType())) {
            throw new RuleValidationException("Rule type cannot be changed after creation");
        }

        // Validate state transitions
        validateStateTransition(existingRule.getStatus(), updatedRule.getStatus());

        // Validate version control
        if (existingRule.getVersion() >= updatedRule.getVersion()) {
            throw new RuleValidationException("Rule version must be incremented");
        }

        // Validate modification of active rules
        if (existingRule.getIsActive() && hasSignificantChanges(existingRule, updatedRule)) {
            throw new RuleValidationException("Cannot make significant changes to active rules");
        }
    }

    /**
     * Validates numeric values according to business rules
     * @param value The numeric value to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @param fieldName Name of the field being validated
     * @throws RuleValidationException if validation fails
     */
    public void validateNumericValue(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) throws RuleValidationException {
        if (value == null) {
            throw new RuleValidationException(fieldName + " cannot be null");
        }

        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new RuleValidationException(String.format(
                    "%s must be between %s and %s", fieldName, min, max
            ));
        }

        // Validate decimal places
        if (value.scale() > 2) {
            throw new RuleValidationException(fieldName + " cannot have more than 2 decimal places");
        }
    }

    /**
     * Validates integer values according to business rules
     * @param value The integer value to validate
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @param fieldName Name of the field being validated
     * @throws RuleValidationException if validation fails
     */
    public void validateIntegerValue(Integer value, int min, int max, String fieldName) throws RuleValidationException {
        if (value == null) {
            throw new RuleValidationException(fieldName + " cannot be null");
        }

        if (value < min || value > max) {
            throw new RuleValidationException(String.format(
                    "%s must be between %d and %d", fieldName, min, max
            ));
        }
    }

    /**
     * Validates time values and time ranges
     * @param startTime Start time to validate
     * @param endTime End time to validate
     * @throws RuleValidationException if validation fails
     */
    public void validateTimeValue(LocalTime startTime, LocalTime endTime) throws RuleValidationException {
        if (startTime == null || endTime == null) {
            throw new RuleValidationException("Time values cannot be null");
        }

        // Validate time format
        try {
            if (!TIME_PATTERN.matcher(startTime.toString()).matches() ||
                    !TIME_PATTERN.matcher(endTime.toString()).matches()) {
                throw new RuleValidationException("Invalid time format. Use HH:mm");
            }
        } catch (DateTimeParseException e) {
            throw new RuleValidationException("Invalid time format: " + e.getMessage());
        }

        // Validate time range
        if (!startTime.isBefore(endTime)) {
            throw new RuleValidationException("Start time must be before end time");
        }

        // Validate business hours
        LocalTime businessStart = LocalTime.of(8, 0);
        LocalTime businessEnd = LocalTime.of(20, 0);
        if (startTime.isBefore(businessStart) || endTime.isAfter(businessEnd)) {
            throw new RuleValidationException("Time range must be within business hours (08:00-20:00)");
        }
    }

    /**
     * Validates competitor-related values and rules
     * @param competitorId ID of the competitor to validate
     * @param competitorRule Rule associated with the competitor
     * @throws RuleValidationException if validation fails
     */
    public void validateCompetitorValue(Long competitorId, CompetitorRule competitorRule) throws RuleValidationException {
        if (competitorId == null) {
            throw new RuleValidationException("Competitor ID cannot be null");
        }

        // Validate competitor exists
        if (!competitorService.existsById(competitorId)) {
            throw new RuleValidationException("Invalid competitor ID: " + competitorId);
        }

        // Validate competitor status
        if (!competitorService.isActive(competitorId)) {
            throw new RuleValidationException("Competitor is not active: " + competitorId);
        }

        // Validate competitor rule specific constraints
        if (competitorRule != null) {
            validateCompetitorRuleConstraints(competitorRule);
        }
    }

    /**
     * Validates action sequences in rules
     * @param actions List of actions to validate
     * @throws RuleValidationException if validation fails
     */
    public void validateActionSequences(List<RuleAction> actions) throws RuleValidationException {
        if (actions == null || actions.isEmpty()) {
            throw new RuleValidationException("Rule must have at least one action");
        }

        if (actions.size() > MAX_ACTIONS_PER_RULE) {
            throw new RuleValidationException("Rule cannot have more than " + MAX_ACTIONS_PER_RULE + " actions");
        }

        // Validate action order
        validateActionOrder(actions);

        // Validate action dependencies
        validateActionDependencies(actions);

        // Validate action conflicts
        validateActionConflicts(actions);
    }

    /**
     * Validates custom actions in rules
     * @param action Custom action to validate
     * @throws RuleValidationException if validation fails
     */
    public void validateCustomAction(CustomAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Custom action cannot be null");
        }

        // Validate custom action script
        if (action.getScript() == null || action.getScript().trim().isEmpty()) {
            throw new RuleValidationException("Custom action script cannot be empty");
        }

        // Validate script syntax
        validateScriptSyntax(action.getScript());

        // Validate script security
        validateScriptSecurity(action.getScript());

        // Validate script parameters
        validateScriptParameters(action.getParameters());
    }

    /**
     * Validates price setting actions
     * @param action Price setting action to validate
     * @throws RuleValidationException if validation fails
     */
    public void validateSetPriceAction(SetPriceAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Set price action cannot be null");
        }

        // Validate price value
        validateNumericValue(
                action.getPrice(),
                MIN_PRICE,
                action.getMaxAllowedPrice(),
                "Price"
        );

        // Validate price change percentage
        BigDecimal changePercentage = calculatePriceChangePercentage(
                action.getCurrentPrice(),
                action.getPrice()
        );
        if (changePercentage.abs().compareTo(MAX_PRICE_CHANGE_PERCENTAGE) > 0) {
            throw new RuleValidationException("Price change exceeds maximum allowed percentage");
        }

        // Validate price constraints
        validatePriceConstraints(action);
    }

    /**
     * Validates discount actions in rules
     * @param action Discount action to validate
     * @throws RuleValidationException if validation fails
     */
    public void validateDiscountAction(DiscountAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Discount action cannot be null");
        }

        // Validate discount percentage
        validateNumericValue(
                action.getDiscountPercentage(),
                BigDecimal.ZERO,
                new BigDecimal("100"),
                "Discount percentage"
        );

        // Validate discount duration
        if (action.getDuration() > 0) {
            validateIntegerValue(
                    action.getDuration(),
                    1,
                    90,
                    "Discount duration"
            );
        }

        // Validate discount constraints
        validateDiscountConstraints(action);

        // Validate discount stacking
        validateDiscountStacking(action);
    }

    private void validateDiscountAction(PricingRule rule, BigDecimal maxDiscount) throws RuleValidationException {
        for (RuleAction action : rule.getActions()) {
            if (action instanceof DiscountAction) {
                DiscountAction discountAction = (DiscountAction) action;
                validateDiscountValue(discountAction.getDiscountValue(), discountAction.getDiscountType());
                
                if (discountAction.isStackable()) {
                    validateStackableDiscount(discountAction);
                } else {
                    validateNonStackableDiscount(discountAction);
                }

                if (discountAction.getMinimumPurchaseAmount() != null) {
                    validateNumericValue(
                        discountAction.getMinimumPurchaseAmount(),
                        BigDecimal.ZERO,
                        null,
                        "minimum purchase amount"
                    );
                }

                if (discountAction.getMaximumDiscountAmount() != null) {
                    validateNumericValue(
                        discountAction.getMaximumDiscountAmount(),
                        BigDecimal.ZERO,
                        maxDiscount,
                        "maximum discount amount"
                    );
                }

                if (discountAction.isSeasonalRestrictions()) {
                    validateSeasonalRestrictions(discountAction);
                }
            }
        }
    }

    private void validateDiscountAgainstLimit(PricingRule rule, BigDecimal maxDiscount) throws RuleValidationException {
        for (RuleAction action : rule.getActions()) {
            if (action instanceof DiscountAction) {
                DiscountAction discountAction = (DiscountAction) action;
                BigDecimal totalDiscount = calculatePotentialTotalDiscount(discountAction);
                
                if (totalDiscount.compareTo(maxDiscount) > 0) {
                    throw new RuleValidationException(
                        String.format("Total discount %.2f exceeds maximum allowed %.2f",
                            totalDiscount, maxDiscount)
                    );
                }
            }
        }
    }

    private void validateStackableDiscount(DiscountAction action) throws RuleValidationException {
        if (action.getMaxStackCount() <= 0) {
            throw new RuleValidationException("Maximum stack count must be greater than zero");
        }
        if (action.getMaxStackCount() > MAX_STACK_COUNT) {
            throw new RuleValidationException("Maximum stack count cannot exceed " + MAX_STACK_COUNT);
        }
        validateStackInterval(action.getStackInterval());
    }

    private void validateStackInterval(String stackInterval) throws RuleValidationException {
        if (stackInterval == null || stackInterval.trim().isEmpty()) {
            throw new RuleValidationException("Stack interval cannot be null or empty");
        }
        if (!TIME_PATTERN.matcher(stackInterval).matches()) {
            throw new RuleValidationException("Invalid stack interval format. Expected format: HH:mm");
        }
    }

    // Private helper methods

    private void validateStateTransition(RuleStatus currentStatus, RuleStatus newStatus) throws RuleValidationException {
        Set<RuleStatus> allowedTransitions = getAllowedStateTransitions(currentStatus);
        if (!allowedTransitions.contains(newStatus)) {
            throw new RuleValidationException(
                    String.format("Invalid state transition from %s to %s", currentStatus, newStatus)
            );
        }
    }

    private Set<RuleStatus> getAllowedStateTransitions(RuleStatus currentStatus) {
    }

    private void validateCompetitorRuleConstraints(CompetitorRule rule) throws RuleValidationException {
        validateNumericValue(
                rule.getPriceThreshold(),
                BigDecimal.ZERO,
                new BigDecimal("1000000"),
                "Price threshold"
        );

        if (rule.getExcludedProducts() != null && rule.getExcludedProducts().size() > 100) {
            throw new RuleValidationException("Too many excluded products");
        }
    }

    private void validateActionOrder(List<RuleAction> actions) {
        Set<Integer> orders = new HashSet<>();
        for (RuleAction action : actions) {
            if (!orders.add(action.getOrder())) {
                throw new RuleValidationException("Duplicate action order: " + action.getOrder());
            }
        }
    }

    private void validateActionDependencies(List<RuleAction> actions) {
        for (RuleAction action : actions) {
            if (action.getDependsOn() != null) {
                boolean dependencyFound = actions.stream()
                        .anyMatch(a -> a.getId().equals(action.getDependsOn()));
                if (!dependencyFound) {
                    throw new RuleValidationException(
                            "Action depends on non-existent action: " + action.getDependsOn()
                    );
                }
            }
        }
    }

    private void validateActionConflicts(List<RuleAction> actions) throws RuleValidationException {
        // Check for conflicting price actions
        long priceActionCount = actions.stream()
                .filter(a -> a instanceof SetPriceAction || a instanceof DiscountAction)
                .count();
        if (priceActionCount > 1) {
            throw new RuleValidationException("Multiple price-related actions are not allowed");
        }
    }

    private void validateScriptSyntax(String script) throws RuleValidationException {
        // Add script syntax validation logic
        // This is a placeholder for actual script validation
        if (!script.contains("return")) {
            throw new RuleValidationException("Script must contain a return statement");
        }
    }

    private void validateScriptSecurity(String script) throws RuleValidationException {
        // Add script security validation logic
        List<String> forbiddenKeywords = Arrays.asList("System.", "Runtime.", "Process");
        for (String keyword : forbiddenKeywords) {
            if (script.contains(keyword)) {
                throw new RuleValidationException("Script contains forbidden keyword: " + keyword);
            }
        }
    }

    private void validateScriptParameters(Map<String, Object> parameters) throws RuleValidationException {
        if (parameters == null) {
            return;
        }

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
                throw new RuleValidationException("Parameter key cannot be empty");
            }
            if (entry.getValue() == null) {
                throw new RuleValidationException("Parameter value cannot be null: " + entry.getKey());
            }
        }
    }

    private void validatePriceConstraints(SetPriceAction action) throws RuleValidationException {
        // Validate minimum margin
        if (action.getMinMarginPercentage() != null) {
            BigDecimal costPrice = action.getCostPrice();
            BigDecimal newPrice = action.getPrice();
            BigDecimal margin = newPrice.subtract(costPrice)
                    .divide(newPrice, 2, BigDecimal.ROUND_HALF_UP)
                    .multiply(new BigDecimal("100"));

            if (margin.compareTo(action.getMinMarginPercentage()) < 0) {
                throw new RuleValidationException("Price violates minimum margin requirement");
            }
        }
    }

    private void validateDiscountConstraints(DiscountAction action) throws RuleValidationException {
        // Validate minimum price after discount
        BigDecimal priceAfterDiscount = action.getCurrentPrice()
                .multiply(BigDecimal.ONE.subtract(
                        action.getDiscountPercentage().divide(new BigDecimal("100"))
                ));

        if (priceAfterDiscount.compareTo(MIN_PRICE) < 0) {
            throw new RuleValidationException(
                    "Discount would result in price below minimum allowed"
            );
        }
    }

    private void validateDiscountStacking(DiscountAction action) throws RuleValidationException {
        if (action == null) {
            throw new RuleValidationException("Discount action cannot be null");
        }

        if (action.isStackable()) {
            // Validate stack count
            if (action.getMaxStackCount() <= 0) {
                throw new RuleValidationException("Maximum stack count must be greater than zero");
            }
            if (action.getMaxStackCount() > MAX_STACK_COUNT) {
                throw new RuleValidationException("Maximum stack count cannot exceed " + MAX_STACK_COUNT);
            }

            // Validate stack interval
            String stackInterval = action.getStackInterval();
            if (stackInterval == null || stackInterval.trim().isEmpty()) {
                throw new RuleValidationException("Stack interval is required for stackable discounts");
            }
            if (!TIME_PATTERN.matcher(stackInterval).matches()) {
                throw new RuleValidationException("Invalid stack interval format. Expected format: HH:mm");
            }

            // Calculate and validate total potential discount
            BigDecimal maxPotentialDiscount;
            if ("PERCENTAGE".equals(action.getDiscountType())) {
                maxPotentialDiscount = action.getDiscountValue()
                    .multiply(new BigDecimal(action.getMaxStackCount()))
                    .divide(new BigDecimal("100"), 2, BigDecimal.ROUND_HALF_UP);
                
                if (maxPotentialDiscount.compareTo(new BigDecimal("100")) > 0) {
                    throw new RuleValidationException(
                        String.format("Maximum potential stacked discount (%.2f%%) exceeds 100%%", 
                            maxPotentialDiscount)
                    );
                }
            } else {
                // For fixed amount discounts
                maxPotentialDiscount = action.getDiscountValue()
                    .multiply(new BigDecimal(action.getMaxStackCount()));
                
                if (action.getMaximumDiscountAmount() != null && 
                    maxPotentialDiscount.compareTo(action.getMaximumDiscountAmount()) > 0) {
                    throw new RuleValidationException(
                        String.format("Maximum potential stacked discount (%.2f) exceeds maximum allowed discount (%.2f)", 
                            maxPotentialDiscount, action.getMaximumDiscountAmount())
                    );
                }
            }

            // Validate minimum purchase requirements
            if (action.getMinimumPurchaseAmount() != null) {
                BigDecimal effectiveMinPurchase = action.getMinimumPurchaseAmount()
                    .multiply(new BigDecimal(action.getMaxStackCount()));
                
                if (effectiveMinPurchase.compareTo(new BigDecimal("10000")) > 0) {  // Example threshold
                    throw new RuleValidationException(
                        String.format("Total minimum purchase requirement for maximum stacks (%.2f) is too high", 
                            effectiveMinPurchase)
                    );
                }
            }

            // Validate duration if present
            if (action.getDuration() > 0) {
                validateStackDuration(action);
            }
        } else {
            // Validate non-stackable discount
            if (action.getMaxStackCount() != 1) {
                throw new RuleValidationException("Non-stackable discounts must have max stack count of 1");
            }
            if (action.getStackInterval() != null && !action.getStackInterval().trim().isEmpty()) {
                throw new RuleValidationException("Non-stackable discounts should not have stack interval");
            }
        }
    }

    private void validateStackDuration(DiscountAction action) throws RuleValidationException {
        if (action.getDuration() < 0) {
            throw new RuleValidationException("Stack duration cannot be negative");
        }
        
        int maxAllowedDuration = 24 * 60; // 24 hours in minutes
        if (action.getDuration() > maxAllowedDuration) {
            throw new RuleValidationException(
                String.format("Stack duration (%d minutes) cannot exceed %d minutes", 
                    action.getDuration(), maxAllowedDuration)
            );
        }

        if (action.getEndDate() != null && 
            action.getEndDate().isBefore(Instant.now().plusSeconds(action.getDuration() * 60L))) {
            throw new RuleValidationException("Stack duration extends beyond the discount end date");
        }
    }

    private BigDecimal calculatePotentialTotalDiscount(DiscountAction action) {
        BigDecimal baseDiscount = action.getDiscountPercentage();
        if (!action.isStackable()) {
            return baseDiscount;
        }

        return baseDiscount.multiply(new BigDecimal(action.getMaxStackCount()));
    }

    private void validateFinalPriceAfterDiscounts(DiscountAction action, BigDecimal totalDiscountPercentage) throws RuleValidationException {
        BigDecimal originalPrice = action.getCurrentPrice();
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                totalDiscountPercentage.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
        );
        BigDecimal finalPrice = originalPrice.multiply(discountMultiplier);

        if (finalPrice.compareTo(MIN_ALLOWED_PRICE) < 0) {
            throw new RuleValidationException(
                    "Final price after all discounts would be below minimum allowed price"
            );
        }
    }

    private void validateMarginAfterDiscounts(DiscountAction action, BigDecimal totalDiscountPercentage) throws RuleValidationException {
        if (action.getCostPrice() == null) {
            return;
        }

        BigDecimal originalPrice = action.getCurrentPrice();
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                totalDiscountPercentage.divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP)
        );
        BigDecimal finalPrice = originalPrice.multiply(discountMultiplier);
        BigDecimal finalMargin = finalPrice.subtract(action.getCostPrice())
                .divide(finalPrice, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));

        if (finalMargin.compareTo(MIN_MARGIN_PERCENTAGE) < 0) {
            throw new RuleValidationException(
                    "Margin after all discounts would be below minimum allowed percentage"
            );
        }
    }

    private void validateDiscountTimeRestrictions(TimeRestrictions restrictions) throws RuleValidationException {
        // Validate business hours
        if (restrictions.getStartTime() != null && restrictions.getEndTime() != null) {
            validateTimeValue(restrictions.getStartTime(), restrictions.getEndTime());
        }

        // Validate days of week
        if (restrictions.getDaysOfWeek() != null && restrictions.getDaysOfWeek().isEmpty()) {
            throw new RuleValidationException("Days of week restrictions cannot be empty if specified");
        }

        // Validate blackout dates
        if (restrictions.getBlackoutDates() != null) {
            validateBlackoutDates(restrictions.getBlackoutDates());
        }
    }

    private void validateSeasonalRestrictions(DiscountAction action) throws RuleValidationException {
        if (action.getSeasonalRestrictions()) {
            return;
        }

        // Validate season dates
        if (action.getSeasonalRestrictions().getSeasonStart() != null &&
                action.getSeasonalRestrictions().getSeasonEnd() != null) {
            if (action.getSeasonalRestrictions().getSeasonStart()
                    .isAfter(action.getSeasonalRestrictions().getSeasonEnd())) {
                throw new RuleValidationException("Season start date must be before season end date");
            }
        }

        // Validate holiday restrictions
        if (action.getSeasonalRestrictions().getHolidays() != null) {
            validateSeasonalRestrictions(action.getSeasonalRestrictions().getHolidays());
        }
    }

    private BigDecimal calculatePriceChangePercentage(BigDecimal oldPrice, BigDecimal newPrice) {
        return newPrice.subtract(oldPrice)
                .divide(oldPrice, 2, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}