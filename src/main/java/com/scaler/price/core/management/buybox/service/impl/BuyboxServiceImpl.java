package com.scaler.price.core.management.buybox.service.impl;

import com.scaler.price.core.management.buybox.service.BuyboxService;
import com.scaler.price.core.management.domain.Price;
import com.scaler.price.core.management.repository.PriceRepository;
import com.scaler.price.core.management.service.SellerScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class BuyboxServiceImpl implements BuyboxService {
    private final PriceRepository priceRepository;
    private final SellerScoreService sellerScoreService;  // For seller metrics

    @Value("${buybox.weights.price}")
    private double priceWeight;

    @Value("${buybox.weights.sellerRating}")
    private double sellerRatingWeight;

    @Value("${buybox.weights.fulfillment}")
    private double fulfillmentWeight;

    @Value("${buybox.weights.stock}")
    private double stockWeight;
    
    @Override
    public Optional<Price> getWinningPrice(Long productId, Long siteId) {
        log.debug("Finding winning price for product: {}, site: {}", productId, siteId);
        
        List<Price> eligiblePrices = getEligiblePrices(productId, siteId);
        if (eligiblePrices.isEmpty()) {
            log.debug("No eligible prices found for product: {}", productId);
            return Optional.empty();
        }
        
        return determineBuyboxWinner(eligiblePrices);
    }



    public List<Price> getEligiblePrices(Long productId, Long siteId) {
        LocalDateTime now = LocalDateTime.now();
        return priceRepository.findActivePricesForBuybox(
                productId,
                siteId,
                now,
                true,  // isActive
                true,  // isSellerActive
                true   // isSiteActive
        );
    }
    
    private Optional<Price> determineBuyboxWinner(List<Price> eligiblePrices) {
        return eligiblePrices.stream()
                .map(price -> new BuyboxScore(price, calculateScore(price)))
                .max(Comparator.comparing(BuyboxScore::getScore))
                .map(BuyboxScore::getPrice);
    }
    
    private double calculateScore(Price price) {
        double score = 0.0;

        // Price component (40% weight)
        score += calculatePriceScore(price) * priceWeight;
        // Seller rating component (30% weight)
        score += sellerScoreService.getSellerRating(price.getSellerId()) * sellerRatingWeight;
        // Fulfillment score (20% weight)
        score += calculateFulfillmentScore(price) * fulfillmentWeight;
        // Stock availability (10% weight)
        score += calculateStockScore(price) * stockWeight;
        
        log.debug("Calculated buybox score {} for price: {}", score, price);
        return score;
    }
    
    private double calculatePriceScore(Price price) {
        // Lower price gets higher score
        BigDecimal mrp = price.getMrp();
        BigDecimal sellingPrice = price.getSellingPrice();
        
        if (mrp.compareTo(BigDecimal.ZERO) <= 0) {
            return 0.0;
        }
        
        // Calculate discount percentage
        double discountPercent = mrp.subtract(sellingPrice)
                .divide(mrp, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
                
        // Score based on discount percentage
        return Math.min(discountPercent / 50.0, 1.0); // Normalize to 0-1
    }
    
    private double calculateFulfillmentScore(Price price) {
        if (price.getFulfilmentType() == null) {
            return 0.0;
        }
        
        return switch (price.getFulfilmentType()) {
            case PRIME -> 1.0;
            case EXPRESS -> 0.8;
            case STANDARD -> 0.6;
            default -> 0.4;
        };
    }
    
    private double calculateStockScore(Price price) {
        // Implement based on your inventory system
        // This is a placeholder implementation
        return 1.0;
    }
    
    @lombok.Value
    private static class BuyboxScore {
        Price price;
        double score;
    }
} 