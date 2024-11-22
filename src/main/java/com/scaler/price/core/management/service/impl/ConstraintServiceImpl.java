package com.scaler.price.core.management.service.impl;

import com.scaler.price.audit.AuditEventPublisher;
import com.scaler.price.core.management.repository.CategoryAttributesRepository;
import com.scaler.price.core.management.repository.MarginConstraintsRepository;
import com.scaler.price.core.management.repository.PriceConstraintsRepository;
import com.scaler.price.core.management.repository.TimeConstraintsRepository;
import com.scaler.price.core.management.service.ConstraintService;
import com.scaler.price.rule.domain.PricingRule;
import com.scaler.price.rule.domain.constraint.MarginConstraints;
import com.scaler.price.rule.domain.constraint.PriceConstraints;
import com.scaler.price.rule.domain.constraint.RuleConstraints;
import com.scaler.price.rule.domain.constraint.TimeConstraints;
import com.scaler.price.rule.dto.CategoryAttributes;
import com.scaler.price.rule.repository.RuleRepository;
import com.scaler.price.validation.helper.ActionParameters;
import com.scaler.price.validation.services.TimeValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConstraintServiceImpl implements ConstraintService {

    private final RuleRepository ruleRepository;
    private final TimeValidator timeValidator;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private TimeConstraintsRepository timeConstraintsRepository;
    private AuditEventPublisher auditEventPublisher;
    private PriceConstraintsRepository priceConstraintsRepository;
    private MarginConstraintsRepository marginConstraintsRepository;
    private CategoryAttributesRepository categoryAttributesRepository;


    @Override
    public List<String> validateRuleConstraints(PricingRule rule) {
        List<String> violations = new ArrayList<>();

        if (rule == null) {
            violations.add("Rule cannot be null");
            return violations;
        }

        // Validate basic rule properties
        if (rule.getRuleName() == null || rule.getRuleName().trim().isEmpty()) {
            violations.add("Rule name is required");
        }

        if (rule.getPriority() == null) {
            violations.add("Rule priority is required");
        }

        // Validate rule conditions and actions
        if (rule.getConditions() == null || rule.getConditions().isEmpty()) {
            violations.add("At least one condition is required");
        }

        if (rule.getActions() == null || rule.getActions().isEmpty()) {
            violations.add("At least one action is required");
        }

        return violations;
    }

    @Override
    public List<String> validatePriceConstraints(BigDecimal price, ActionParameters parameters) {
        List<String> violations = new ArrayList<>();

        if (price == null) {
            violations.add("Price cannot be null");
            return violations;
        }

        if (price.compareTo(BigDecimal.ZERO) < 0) {
            violations.add("Price cannot be negative");
        }

        if (parameters.getMaxPriceIncrease() != null &&
                price.subtract(parameters.getMaxPriceIncrease()).compareTo(BigDecimal.ZERO) > 0) {
            violations.add("Price increase exceeds maximum allowed increase");
        }

        return violations;
    }

    @Override
    public List<String> validateMarginConstraints(BigDecimal margin, ActionParameters parameters) {
        List<String> violations = new ArrayList<>();

        if (margin == null) {
            violations.add("Margin cannot be null");
            return violations;
        }

        if (parameters.getMinimumMargin() != null &&
                margin.compareTo(parameters.getMinimumMargin()) < 0) {
            violations.add("Margin is below minimum allowed margin");
        }

        if (parameters.getMaximumMargin() != null &&
                margin.compareTo(parameters.getMaximumMargin()) > 0) {
            violations.add("Margin exceeds maximum allowed margin");
        }

        return violations;
    }

    @Override
    public List<String> validateDiscountStacking(List<BigDecimal> existingDiscounts,
                                                 BigDecimal newDiscount,
                                                 ActionParameters parameters) {
        List<String> violations = new ArrayList<>();

        if (newDiscount == null) {
            violations.add("New discount cannot be null");
            return violations;
        }

        if (!parameters.getStackable()) {
            if (!existingDiscounts.isEmpty()) {
                violations.add("Discount stacking is not allowed");
            }
            return violations;
        }

        BigDecimal totalDiscount = existingDiscounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(newDiscount);

        if (parameters.getMaxDiscountAmount() != null &&
                totalDiscount.compareTo(parameters.getMaxDiscountAmount()) > 0) {
            violations.add("Total discount exceeds maximum allowed amount");
        }

        if (parameters.getMaxDiscountPercent() != null &&
                totalDiscount.compareTo(parameters.getMaxDiscountPercent()) > 0) {
            violations.add("Total discount exceeds maximum allowed percentage");
        }

        return violations;
    }

    @Override
    public boolean isTimeConstraintSatisfied(PricingRule rule) {
        LocalDateTime now = LocalDateTime.now();
        return timeValidator.isValidTime(rule.getStartDate(), rule.getEndDate(), now);
    }

    @Override
    public List<String> validateInventoryConstraints(String productId,
                                                     Integer quantity,
                                                     ActionParameters parameters) {
        List<String> violations = new ArrayList<>();

        if (quantity == null || quantity < 0) {
            violations.add("Invalid quantity specified");
            return violations;
        }

        // Add inventory-specific validations based on parameters
        if (parameters.getMinQuantity() != null &&
                quantity < Integer.parseInt(parameters.getMinQuantity())) {
            violations.add("Quantity is below minimum required quantity");
        }

        if (parameters.getMaxQuantity() != null &&
                quantity > Integer.parseInt(parameters.getMaxQuantity())) {
            violations.add("Quantity exceeds maximum allowed quantity");
        }

        return violations;
    }

    @Override
    public List<String> validateCategoryConstraints(String categoryId, PricingRule rule) {
        List<String> violations = new ArrayList<>();

        if (categoryId == null || categoryId.trim().isEmpty()) {
            violations.add("Category ID is required");
            return violations;
        }

        // Add category-specific validations
        Set<String> allowedCategories = rule.getAllowedCategories();
        if (allowedCategories != null && !allowedCategories.isEmpty() &&
                !allowedCategories.contains(categoryId)) {
            violations.add("Rule is not applicable for this category");
        }

        return violations;
    }

    @Override
    public List<String> validateCustomerSegmentConstraints(String customerId, PricingRule rule) {
        List<String> violations = new ArrayList<>();

        if (customerId == null || customerId.trim().isEmpty()) {
            violations.add("Customer ID is required");
            return violations;
        }

        // Add customer segment specific validations
        Set<String> allowedSegments = rule.getAllowedCustomerSegments();
        if (allowedSegments != null && !allowedSegments.isEmpty() &&
                !allowedSegments.contains(getCustomerSegment(customerId))) {
            violations.add("Rule is not applicable for this customer segment");
        }

        return violations;
    }

    @Override
    public List<String> validateChannelConstraints(String channelId, PricingRule rule) {
        List<String> violations = new ArrayList<>();

        if (channelId == null || channelId.trim().isEmpty()) {
            violations.add("Channel ID is required");
            return violations;
        }

        Set<String> allowedChannels = rule.getAllowedChannels();
        if (allowedChannels != null && !allowedChannels.isEmpty() &&
                !allowedChannels.contains(channelId)) {
            violations.add("Rule is not applicable for this channel");
        }

        return violations;
    }

    @Override
    public List<String> validateGeographicalConstraints(Map<String, String> location, PricingRule rule) {
        List<String> violations = new ArrayList<>();

        if (location == null || location.isEmpty()) {
            violations.add("Location information is required");
            return violations;
        }

        // Add geographical constraint validations
        Set<String> allowedRegions = rule.getAllowedRegions();
        if (allowedRegions != null && !allowedRegions.isEmpty() &&
                !allowedRegions.contains(location.get("region"))) {
            violations.add("Rule is not applicable for this region");
        }

        return violations;
    }

    @Override
    public PricingRule addConstraint(PricingRule rule, RuleConstraints constraint) {
        if (rule == null || constraint == null) {
            throw new IllegalArgumentException("Rule and constraint cannot be null");
        }

        rule.getConstraints().add(constraint);
        return ruleRepository.save(rule);
    }

    @Override
    public PricingRule removeConstraint(PricingRule rule, String constraintId) {
        if (rule == null || constraintId == null) {
            throw new IllegalArgumentException("Rule and constraint ID cannot be null");
        }

        rule.setConstraints(rule.getConstraints().stream()
                .filter(c -> !c.getId().equals(constraintId))
                .collect(Collectors.toSet()));
        return ruleRepository.save(rule);
    }

    @Override
    @Transactional
    public PricingRule updateRuleConstraint(Long ruleId, RuleConstraints constraint) {
        if (ruleId == null || constraint == null) {
            throw new IllegalArgumentException("Rule ID and constraint cannot be null");
        }

        PricingRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new EntityNotFoundException("Rule not found with id: " + ruleId));

        // Initialize constraints set if null
        if (rule.getConstraints() == null) {
            rule.setConstraints(new HashSet<>());
        }

        // AuditEntryexisting constraint of the same type if exists
        rule.getConstraints().removeIf(c -> c.getClass().equals(constraint.getClass()));

        // Add the new constraint
        rule.getConstraints().add(constraint);

        // Validate the updated constraints
        List<String> violations = validateRuleConstraints(rule);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Invalid constraints: " + String.join(", ", violations));
        }

        try {
            PricingRule savedRule = ruleRepository.save(rule);
            log.debug("Successfully updated constraints for rule: {}", ruleId);
            
            // Get user ID from request context or use a default value
            String userId = getCurrentUserId();
            
            // Audit the change
            auditEventPublisher.publishRuleModifiedEvent(
                savedRule,
                userId,
                Collections.singletonMap("constraintUpdate", 
                    constraint.getClass().getSimpleName() + " updated")
            );
            
            return savedRule;
        } catch (Exception e) {
            log.error("Error updating rule constraints: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update rule constraints", e);
        }
    }

    @Override
    public PricingRule updateConstraint(PricingRule rule, RuleConstraints constraint) {
        if (rule == null || constraint == null) {
            throw new IllegalArgumentException("Rule and constraint cannot be null");
        }

        rule.setConstraints(rule.getConstraints().stream()
                .map(c -> c.getId().equals(constraint.getId()) ? constraint : c)
                .collect(Collectors.toSet()));
        return ruleRepository.save(rule);
    }

    @Override
    public List<String> validateCompetitorPriceConstraints(String productId,
                                                           BigDecimal price,
                                                           ActionParameters parameters) {
        List<String> violations = new ArrayList<>();

        if (price == null || productId == null) {
            violations.add("Price and product ID are required");
            return violations;
        }

        // Validate competitor price constraints
        if (parameters.getCompetitor() != null) {
            BigDecimal competitorPrice = getCompetitorPrice(productId, parameters.getCompetitor());
            if (competitorPrice != null) {
                if (parameters.getBeatPercentage() != null) {
                    BigDecimal minPrice = competitorPrice.multiply(
                            BigDecimal.ONE.subtract(parameters.getBeatPercentage().divide(HUNDRED)));
                    if (price.compareTo(minPrice) > 0) {
                        violations.add("Price does not meet competitor beat percentage requirement");
                    }
                }

                if (parameters.getMatchExactPrice() != null && parameters.getMatchExactPrice() &&
                        price.compareTo(competitorPrice) != 0) {
                    violations.add("Price does not match competitor price exactly");
                }
            }
        }

        return violations;
    }



    // Helper methods
    private String getCustomerSegment(String customerId) {
        // Implementation to fetch customer segment
        // This should be replaced with actual customer segment logic
        return "REGULAR";
    }

    private BigDecimal getCompetitorPrice(String productId, String competitor) {
        // Implementation to fetch competitor price
        // This should be replaced with actual competitor price fetching logic
        return BigDecimal.ZERO;
    }


    @Override
    @Transactional
    public MarginConstraints setMarginConstraints(MarginConstraints constraints) {
        log.debug("Setting margin constraints: {}", constraints);

        List<String> violations = new ArrayList<>();
        validateMarginConstraints(constraints, violations);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Margin constraints are invalid: " + violations);
        }

        // Check if constraints already exist for the category
        Optional<MarginConstraints> existingConstraints =
                marginConstraintsRepository.findByCategoryId(constraints.getCategoryId());

        MarginConstraints savedConstraints;
        if (existingConstraints.isPresent()) {
            // Update existing constraints
            MarginConstraints existing = existingConstraints.get();
            existing.setMinMarginPercentage(constraints.getMinMarginPercentage());
            existing.setMaxMarginPercentage(constraints.getMaxMarginPercentage());
            existing.setTargetMarginPercentage(constraints.getTargetMarginPercentage());
            existing.setLastModifiedBy(constraints.getLastModifiedBy());
            existing.setLastModifiedDate(constraints.getLastModifiedDate());
            savedConstraints = marginConstraintsRepository.save(existing);

            auditEventPublisher.publishRuleModifiedEvent(
                    null,
                    constraints.getLastModifiedBy(),
                    Map.of("type", "MARGIN_CONSTRAINTS_UPDATE",
                            "categoryId", constraints.getCategoryId().toString())
            );
        } else {
            // Create new constraints
            savedConstraints = marginConstraintsRepository.save(constraints);

            auditEventPublisher.publishRuleCreatedEvent(
                    null,
                    constraints.getLastModifiedBy()
            );
        }

        log.info("Successfully set margin constraints for category {}: {}",
                constraints.getCategoryId(), savedConstraints);
        return savedConstraints;
    }

    @Override
    @Transactional
    public PriceConstraints setPriceConstraints(PriceConstraints constraints) {
        log.debug("Setting price constraints: {}", constraints);

        List<String> violations = new ArrayList<>();
        validatePriceConstraints(constraints, violations);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Price constraints are invalid: " + violations);
        }

        // Check if constraints already exist for the category

        Optional<PriceConstraints> existingConstraints =
                priceConstraintsRepository.findByCategoryId(constraints.getCategoryId());

        PriceConstraints savedConstraints;
        if (existingConstraints.isPresent()) {
            // Update existing constraints
            PriceConstraints existing = existingConstraints.get();
            existing.setMinPrice(constraints.getMinPrice());
            existing.setMaxPrice(constraints.getMaxPrice());
            existing.setMinDiscountPercentage(constraints.getMinDiscountPercentage());
            existing.setMaxDiscountPercentage(constraints.getMaxDiscountPercentage());
            existing.setLastModifiedBy(constraints.getLastModifiedBy());
            existing.setLastModifiedDate(constraints.getLastModifiedDate());
            savedConstraints = priceConstraintsRepository.save(existing);

            auditEventPublisher.publishRuleModifiedEvent(
                    null,
                    constraints.getLastModifiedBy(),
                    Map.of("type", "PRICE_CONSTRAINTS_UPDATE",
                            "categoryId", constraints.getCategoryId().toString())
            );
        } else {
            // Create new constraints
            savedConstraints = priceConstraintsRepository.save(constraints);

            auditEventPublisher.publishRuleCreatedEvent(
                    null,
                    constraints.getLastModifiedBy()
            );
        }

        log.info("Successfully set price constraints for category {}: {}",
                constraints.getCategoryId(), savedConstraints);
        return savedConstraints;
    }

    @Override
    @Transactional
    public TimeConstraints setTimeConstraints(TimeConstraints constraints) {
        log.debug("Setting time constraints: {}", constraints);

        List<String> violations = new ArrayList<>();
        validateTimeConstraints(constraints, violations);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Time constraints are invalid: " + violations);
        }

        // Check if constraints already exist for the category
        Optional<TimeConstraints> existingConstraints =
                timeConstraintsRepository.findByCategoryId(constraints.getCategoryId());

        TimeConstraints savedConstraints;
        if (existingConstraints.isPresent()) {
            // Update existing constraints
            TimeConstraints existing = existingConstraints.get();
            existing.setMinDuration(constraints.getMinDuration());
            existing.setMaxDuration(constraints.getMaxDuration());
            existing.setBlackoutPeriods(constraints.getBlackoutPeriods());
            existing.setLastModifiedBy(constraints.getLastModifiedBy());
            existing.setLastModifiedDate(constraints.getLastModifiedDate());
            savedConstraints = timeConstraintsRepository.save(existing);

            auditEventPublisher.publishRuleModifiedEvent(
                    null,
                    constraints.getLastModifiedBy(),
                    Map.of("type", "TIME_CONSTRAINTS_UPDATE",
                            "categoryId", constraints.getCategoryId().toString())
            );
        } else {
            // Create new constraints
            savedConstraints = timeConstraintsRepository.save(constraints);

            auditEventPublisher.publishRuleCreatedEvent(
                    null,
                    constraints.getLastModifiedBy()
            );
        }

        log.info("Successfully set time constraints for category {}: {}",
                constraints.getCategoryId(), savedConstraints);
        return savedConstraints;
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryAttributes getCategoryConstraints(Long categoryId) {
        log.debug("Retrieving category constraints for categoryId: {}", categoryId);

        if (categoryId == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }

        CategoryAttributes attributes = categoryAttributesRepository
                .findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Category attributes not found for categoryId: " + categoryId));

        // Fetch related constraints
        MarginConstraints marginConstraints = marginConstraintsRepository
                .findByCategoryId(categoryId)
                .orElse(null);

        PriceConstraints priceConstraints = priceConstraintsRepository
                .findByCategoryId(categoryId)
                .orElse(null);

        TimeConstraints timeConstraints = timeConstraintsRepository
                .findByCategoryId(categoryId)
                .orElse(null);

        // Set the fetched constraints
        attributes.setMarginConstraints(marginConstraints);
        attributes.setPriceConstraints(priceConstraints);
        attributes.setTimeConstraints(timeConstraints);

        log.info("Successfully retrieved category constraints for categoryId {}: {}",
                categoryId, attributes);
        return attributes;
    }

    private void validateMarginConstraints(MarginConstraints constraints, List<String> violations) {
        if (constraints == null) {
            violations.add("Margin constraints cannot be null");
            return;
        }
        if (constraints.getCategoryId() == null) {
            violations.add("Category ID cannot be null");
        }
        if (constraints.getMinMarginPercentage() != null &&
                constraints.getMaxMarginPercentage() != null &&
                constraints.getMinMarginPercentage().compareTo(constraints.getMaxMarginPercentage()) > 0
                 ) {
            violations.add("Minimum margin percentage cannot be greater than maximum margin percentage");
        }
    }

    private void validatePriceConstraints(PriceConstraints constraints, List<String> violations) {
        if (constraints == null) {
            violations.add("Price constraints cannot be null");
            return;
        }
        if (constraints.getCategoryId() == null) {
            violations.add("Category ID cannot be null");
        }
        if (constraints.getMinPrice() != null &&
                constraints.getMaxPrice() != null &&
                constraints.getMinPrice().compareTo(constraints.getMaxPrice()) > 0) {
            violations.add("Minimum price cannot be greater than maximum price");
        }
        if (constraints.getMinDiscountPercentage() != null &&
                constraints.getMaxDiscountPercentage() != null &&
                constraints.getMinDiscountPercentage() > constraints.getMaxDiscountPercentage()) {
            violations.add("Minimum discount percentage cannot be greater than maximum discount percentage");
        }
    }

    private void validateTimeConstraints(TimeConstraints constraints, List<String> violations) {
        if (constraints == null) {
            return;
        }

        // Validate start and end time
        if (constraints.getStartTime() != null && constraints.getEndTime() != null) {
            if (constraints.getStartTime().isAfter(constraints.getEndTime())) {
                violations.add("Start time must be before end time");
            }
        }

        // Validate duration constraints with proper null checks
        if (constraints.getMinDuration() != null && constraints.getMaxDuration() != null) {
            try {
                long minDurationMinutes = parseDurationToMinutes(constraints.getMinDuration());
                long maxDurationMinutes = parseDurationToMinutes(constraints.getMaxDuration());
                
                if (minDurationMinutes > maxDurationMinutes) {
                    violations.add("Minimum duration (" + constraints.getMinDuration() + 
                                 ") cannot be greater than maximum duration (" + 
                                 constraints.getMaxDuration() + ")");
                }
            } catch (IllegalArgumentException e) {
                violations.add("Invalid duration format: " + e.getMessage());
            }
        }

        // Validate individual durations
        if (constraints.getMinDuration() != null) {
            try {
                parseDurationToMinutes(constraints.getMinDuration());
            } catch (IllegalArgumentException e) {
                violations.add("Invalid minimum duration format: " + e.getMessage());
            }
        }

        if (constraints.getMaxDuration() != null) {
            try {
                parseDurationToMinutes(constraints.getMaxDuration());
            } catch (IllegalArgumentException e) {
                violations.add("Invalid maximum duration format: " + e.getMessage());
            }
        }
    }

    /**
     * Parses duration string to minutes
     * Supports formats: "1h", "30m", "1h30m", "90m", etc.
     */
    private long parseDurationToMinutes(String duration) {
        if (duration == null || duration.trim().isEmpty()) {
            throw new IllegalArgumentException("Duration cannot be empty");
        }

        duration = duration.toLowerCase().trim();
        long totalMinutes = 0;

        // Handle hours
        if (duration.contains("h")) {
            String[] parts = duration.split("h");
            try {
                totalMinutes = Long.parseLong(parts[0]) * 60;
                duration = parts.length > 1 ? parts[1] : "";
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid hours format");
            }
        }

        // Handle minutes
        if (duration.contains("m")) {
            String[] parts = duration.split("m");
            try {
                totalMinutes += Long.parseLong(parts[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid minutes format");
            }
        } else if (!duration.isEmpty()) {
            try {
                totalMinutes += Long.parseLong(duration);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid duration format");
            }
        }

        if (totalMinutes < 0) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        return totalMinutes;
    }

    /**
     * Gets the current user ID from the request context
     * @return The current user ID or a default value if not available
     */
    private String getCurrentUserId() {
        try {
            // Try to get from request context or other authentication mechanism
            return Optional.ofNullable(RequestContextHolder.getRequestAttributes())
                .filter(ServletRequestAttributes.class::isInstance)
                .map(ServletRequestAttributes.class::cast)
                .map(ServletRequestAttributes::getRequest)
                .map(request -> request.getHeader("X-User-ID"))
                .orElse("SYSTEM");
        } catch (Exception e) {
            log.warn("Could not get user ID from request context, using default", e);
            return "SYSTEM";
        }
    }
}