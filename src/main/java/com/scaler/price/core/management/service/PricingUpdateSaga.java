package com.scaler.price.core.management.service;

import com.scaler.price.core.management.domain.Price;
import com.scaler.price.core.management.utils.PriceUpdateCommand;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PricingUpdateSaga {

    private final PriceValidationService validationService;
    private final NotificationService notificationService;
    private final InventoryService inventoryService;;
    private PricingUpdateSaga priceService;

    public PricingUpdateSaga(PriceValidationService validationService, NotificationService notificationService, InventoryService inventoryService) {
        this.validationService = validationService;
        this.notificationService = notificationService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public Price updatePrice(PriceUpdateCommand command) {
        Price updatedPrice = null;
        try {
            // Step 1: Validate Price

            //validationService.validatePrice(command);

            // Step 2: Update Price
            //updatedPrice = priceService.updatePrice(command);

            // Step 3: Update Inventory
            //inventoryService.updatePricing(command);

            // Step 4: Notify
            //notificationService.notifyPriceUpdate(updatedPrice);

        } catch (Exception e) {
            // Compensating transactions
            compensateFailedUpdate(command);
            throw e;
        }
        return updatedPrice;
    }

    private void compensateFailedUpdate(PriceUpdateCommand command) {

    }
}
