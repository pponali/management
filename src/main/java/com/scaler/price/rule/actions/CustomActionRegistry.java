package com.scaler.price.rule.actions;

import com.scaler.price.rule.actions.handler.CustomActionHandler;
import com.scaler.price.rule.actions.handler.LocationPricingHandler;
import com.scaler.price.rule.actions.handler.SeasonalPricingHandler;
import com.scaler.price.rule.dto.CustomActionMetadata;
import com.scaler.price.rule.dto.ParameterSchema;
import com.scaler.price.rule.exceptions.ActionRegistrationException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class CustomActionRegistry {
    private final Map<String, CustomActionHandler> handlers = new ConcurrentHashMap<>();
    private final Map<String, CustomActionMetadata> metadata = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() throws ActionRegistrationException {
        // Register default custom actions
        registerDefaultHandlers();
    }

    public void registerHandler(
            String actionType,
            CustomActionHandler handler,
            CustomActionMetadata metadata) throws ActionRegistrationException {
        log.info("Registering custom action handler: {}", actionType);

        validateHandler(actionType, handler, metadata);

        handlers.put(actionType, handler);
        this.metadata.put(actionType, metadata);

        log.info("Successfully registered custom action: {}", actionType);
    }

    public CustomActionHandler getHandler(String actionType) throws ActionRegistrationException {
        CustomActionHandler handler = handlers.get(actionType);
        if (handler == null) {
            throw new ActionRegistrationException(
                    "No handler registered for action type: " + actionType
            );
        }
        return handler;
    }

    public CustomActionMetadata getMetadata(String actionType) {
        return metadata.get(actionType);
    }

    public Set<String> getRegisteredActionTypes() {
        return handlers.keySet();
    }

    public boolean isActionTypeRegistered(String actionType) {
        return handlers.containsKey(actionType);
    }

    private void validateHandler(
            String actionType,
            CustomActionHandler handler,
            CustomActionMetadata metadata) throws ActionRegistrationException {
        if (actionType == null || actionType.trim().isEmpty()) {
            throw new ActionRegistrationException("Action type cannot be null or empty");
        }

        if (handler == null) {
            throw new ActionRegistrationException("Handler cannot be null");
        }

        if (metadata == null) {
            throw new ActionRegistrationException("Metadata cannot be null");
        }

        if (handlers.containsKey(actionType)) {
            throw new ActionRegistrationException(
                    "Handler already registered for action type: " + actionType
            );
        }
    }

    private void registerDefaultHandlers() throws ActionRegistrationException {
        // Register Season-based Pricing Handler
        registerHandler(
                "SEASON_PRICING",
                new SeasonalPricingHandler(),
                CustomActionMetadata.builder()
                        .name("Seasonal Pricing")
                        .description("Applies seasonal pricing adjustments")
                        .parameterSchema(buildSeasonalPricingSchema())
                        .build()
        );

        // Register Location-based Pricing Handler
        registerHandler(
                "LOCATION_PRICING",
                new LocationPricingHandler(),
                CustomActionMetadata.builder()
                        .name("Location-based Pricing")
                        .description("Applies location-specific price adjustments")
                        .parameterSchema(buildLocationPricingSchema())
                        .build()
        );

        // Add more default handlers as needed
    }

    private ParameterSchema buildLocationPricingSchema() {
        return ParameterSchema.builder()
                .requiredParameters(List.of("location", "priceAdjustment"))
                .optionalParameters(List.of("priceAdjustmentType"))
                .build();
    }

    private ParameterSchema buildSeasonalPricingSchema() {
        return ParameterSchema.builder()
                .requiredParameters(List.of("season", "discount"))
                .optionalParameters(List.of("discountType"))
                .build();
    }
}