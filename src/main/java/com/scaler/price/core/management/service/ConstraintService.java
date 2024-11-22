package com.scaler.price.core.management.service;

import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.scaler.price.validation.helper.ActionParameters;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Service interface for managing and validating pricing rule constraints.
 */
public interface ConstraintService {

    /**
     * Validates if a pricing rule satisfies all defined constraints
     *
     * @param rule The pricing rule to validate
     * @return List of constraint violations, empty if all constraints are satisfied
     */
    List<String> validateRuleConstraints(PricingRule rule);

    /**
     * Validates price-related constraints
     *
     * @param price The price to validate
     * @param parameters Action parameters containing constraint values
     * @return List of constraint violations
     */
    List<String> validatePriceConstraints(BigDecimal price, ActionParameters parameters);

    /**
     * Validates margin-related constraints
     *
     * @param margin The margin to validate
     * @param parameters Action parameters containing constraint values
     * @return List of constraint violations
     */
    List<String> validateMarginConstraints(BigDecimal margin, ActionParameters parameters);

    /**
     * Validates discount stacking constraints
     *
     * @param existingDiscounts List of existing discounts
     * @param newDiscount New discount to be applied
     * @param parameters Action parameters containing constraint values
     * @return List of constraint violations
     */
    List<String> validateDiscountStacking(List<BigDecimal> existingDiscounts,
                                          BigDecimal newDiscount,
                                          ActionParameters parameters);

    /**
     * Checks if a rule can be applied based on time constraints
     *
     * @param rule The pricing rule to check
     * @return true if the rule can be applied, false otherwise
     */
    boolean isTimeConstraintSatisfied(PricingRule rule);

    /**
     * Validates inventory-related constraints
     *
     * @param productId Product identifier
     * @param quantity Quantity to validate
     * @param parameters Action parameters containing constraint values
     * @return List of constraint violations
     */
    List<String> validateInventoryConstraints(String productId,
                                              Integer quantity,
                                              ActionParameters parameters);

    /**
     * Validates category-specific constraints
     *
     * @param categoryId Category identifier
     * @param rule The pricing rule to validate
     * @return List of constraint violations
     */
    List<String> validateCategoryConstraints(String categoryId, PricingRule rule);

    /**
     * Validates customer segment constraints
     *
     * @param customerId Customer identifier
     * @param rule The pricing rule to validate
     * @return List of constraint violations
     */
    List<String> validateCustomerSegmentConstraints(String customerId, PricingRule rule);

    /**
     * Validates channel-specific constraints
     *
     * @param channelId Channel identifier
     * @param rule The pricing rule to validate
     * @return List of constraint violations
     */
    List<String> validateChannelConstraints(String channelId, PricingRule rule);

    /**
     * Validates geographical constraints
     *
     * @param location Location information
     * @param rule The pricing rule to validate
     * @return List of constraint violations
     */
    List<String> validateGeographicalConstraints(Map<String, String> location, PricingRule rule);

    /**
     * Adds a new constraint to a pricing rule
     *
     * @param rule The pricing rule
     * @param constraint The constraint to add
     * @return Updated pricing rule
     */
    PricingRule addConstraint(PricingRule rule, RuleConstraints constraint);

    /**
     * Removes a constraint from a pricing rule
     *
     * @param rule The pricing rule
     * @param constraintId The ID of the constraint to remove
     * @return Updated pricing rule
     */
    PricingRule removeConstraint(PricingRule rule, String constraintId);

    /**
     * Updates an existing constraint
     *
     * @param rule The pricing rule
     * @param constraint The updated constraint
     * @return Updated pricing rule
     */
    PricingRule updateConstraint(PricingRule rule, RuleConstraints constraint);

    /**
     * Validates competitor price constraints
     *
     * @param productId Product identifier
     * @param price Price to validate
     * @param parameters Action parameters containing constraint values
     * @return List of constraint violations
     */
    List<String> validateCompetitorPriceConstraints(String productId,
                                                    BigDecimal price,
                                                    ActionParameters parameters);

    MarginConstraints setMarginConstraints(MarginConstraints constraints);

    PriceConstraints setPriceConstraints(PriceConstraints constraints);

    TimeConstraints setTimeConstraints(TimeConstraints constraints);

    CategoryAttributes getCategoryConstraints(Long categoryId);

    /**
     * Updates a rule constraint with audit logging
     *
     * @param ruleId The ID of the rule to update
     * @param constraint The constraint to update
     * @return Updated pricing rule
     * @throws EntityNotFoundException if rule not found
     * @throws IllegalArgumentException if validation fails
     */
    PricingRule updateRuleConstraint(Long ruleId, RuleConstraints constraint);
}