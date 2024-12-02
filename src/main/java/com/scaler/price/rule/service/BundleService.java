package com.scaler.price.rule.service;

import com.scaler.price.core.management.repository.BundleRepository;
import com.scaler.price.rule.domain.Bundle;
import com.scaler.price.rule.exceptions.BundleNotFoundException;
import com.scaler.price.rule.exceptions.ProductFetchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.scaler.price.rule.domain.DiscountType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BundleService {

    @Autowired
    private BundleRepository bundleRepository;
    private PriceRuleService priceService;

    // Create a new bundle
    public Bundle createBundle(Bundle bundle) {
        return bundleRepository.save(bundle);
    }

    // Retrieve a bundle by ID
    public Optional<Bundle> getBundleById(Long id) {
        return bundleRepository.findById(id);
    }

    // Retrieve all bundles
    public List<Bundle> getAllBundles() {
        return bundleRepository.findAll();
    }

    // Retrieve all bundles with pagination
    public Page<Bundle> getAllBundles(Pageable pageable) {
        return bundleRepository.findAll(pageable);
    }

    // Update an existing bundle
    public Bundle updateBundle(Long id, Bundle bundleDetails) {
        return bundleRepository.findById(id)
                .map(bundle -> {
                    bundle.setName(bundleDetails.getName());
                    bundle.setProducts(bundleDetails.getProducts());
                    return bundleRepository.save(bundle);
                })
                .orElseThrow(() -> new RuntimeException("Bundle not found with id " + id));
    }

    // Delete a bundle
    public void deleteBundle(Long id) {
        bundleRepository.deleteById(id);
    }

    public BundleEligibility checkEligibility(Long bundleId, Long productId, Map<String, Object> attributes) throws ProductFetchException {
        if (bundleId == null) {
            return BundleEligibility.builder()
                    .eligible(false)
                    .ineligibilityReason("Bundle ID cannot be null or empty")
                    .build();
        }

        try {
            Bundle bundle = bundleRepository.findById(bundleId)
                    .orElseThrow(() -> new BundleNotFoundException("Bundle not found with id: " + bundleId));

            // Check if product is in bundle
            if (productId != null && !bundle.getProductIds().contains(productId)) {
                return BundleEligibility.builder()
                        .eligible(false)
                        .ineligibilityReason("Product is not part of the bundle")
                        .build();
            }

            // Calculate bundle price and discount
            BigDecimal originalPrice = priceService.getCurrentPrice(productId);
            BigDecimal discountAmount = calculateBundleDiscount(bundle, originalPrice);
            
            // Check minimum purchase amount
            if (bundle.getMinimumPurchaseAmount() != null && 
                originalPrice.compareTo(bundle.getMinimumPurchaseAmount()) < 0) {
                return BundleEligibility.builder()
                        .eligible(false)
                        .originalPrice(originalPrice)
                        .minimumPurchaseAmount(bundle.getMinimumPurchaseAmount())
                        .ineligibilityReason("Minimum purchase amount not met")
                        .build();
            }

            // Build eligible response
            return BundleEligibility.builder()
                    .eligible(true)
                    .bundleId(bundleId)
                    .originalPrice(originalPrice)
                    .discountAmount(discountAmount)
                    .validUntil(bundle.getValidUntil())
                    .itemCount(bundle.getProductIds().size())
                    .minimumPurchaseAmount(bundle.getMinimumPurchaseAmount())
                    .maximumDiscountAmount(bundle.getMaxDiscount())
                    .marginPercentage(bundle.getMarginPercentage())
                    .build();

        } catch (Exception e) {
            return BundleEligibility.builder()
                    .eligible(false)
                    .ineligibilityReason("Error checking eligibility: " + e.getMessage())
                    .build();
        }
    }

    private BigDecimal calculateBundleDiscount(Bundle bundle, BigDecimal productPrice) {
        BigDecimal discount = BigDecimal.ZERO;

        if (bundle.getDiscountType() == DiscountType.FLAT) {
            discount = bundle.getDiscountAmount();
        } else if (bundle.getDiscountType() == DiscountType.PERCENTAGE) {
            discount = productPrice.multiply(bundle.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Apply maximum discount cap if exists
        if (bundle.getMaxDiscount() != null && discount.compareTo(bundle.getMaxDiscount()) > 0) {
            discount = bundle.getMaxDiscount();
        }

        return discount;
    }

    public BigDecimal getBundleDiscount(Long bundleId, Long productId) {
        // Step 1: Validate input parameters
        if (bundleId == null) {
            throw new IllegalArgumentException("Bundle ID cannot be null or empty");
        }
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        // Step 2: Fetch bundle details
        Bundle bundle = bundleRepository.findById(bundleId)
                .orElseThrow(() -> new BundleNotFoundException("Bundle not found with id: " + bundleId));

        // Step 3: Check if the product is part of the bundle
        if (!bundle.getProducts().contains(productId)) {
            return BigDecimal.ZERO; // No discount if product is not in the bundle
        }

        // Step 4: Calculate the discount
        BigDecimal discount = BigDecimal.ZERO;

        // Example: Flat discount
        if (bundle.getDiscountType() == DiscountType.FLAT) {
            discount = bundle.getDiscountAmount();
        }
        // Example: Percentage discount
        else if (bundle.getDiscountType() == DiscountType.PERCENTAGE) {
            BigDecimal productPrice = priceService.getCurrentPrice(productId);
            discount = productPrice.multiply(bundle.getDiscountPercentage())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // Step 5: Apply any additional business rules
        // For example, cap the discount at a maximum value
        BigDecimal maxDiscount = bundle.getMaxDiscount();
        if (maxDiscount != null && discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    // Retrieve bundles for a specific product
    public List<Bundle> getBundlesForProduct(Long productId) {
        return bundleRepository.findByProductsContaining(productId);
    }
}
