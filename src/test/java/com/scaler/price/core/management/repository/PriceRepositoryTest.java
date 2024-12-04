package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.domain.Price;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PriceRepositoryTest {

    @Autowired
    private PriceRepository priceRepository;

    private Price testPrice;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        testPrice = Price.builder()
                .productId(1L)
                .sellerId(1L)
                .siteId(1L)
                .basePrice(new BigDecimal("100.00"))
                .sellingPrice(new BigDecimal("90.00"))
                .currency("USD")
                .effectiveFrom(now)
                .effectiveTo(now.plusDays(30))
                .isActive(true)
                .build();
        priceRepository.save(testPrice);
    }

    @Test
    void findActivePrice_WhenPriceExists_ShouldReturnPrice() {
        Optional<Price> foundPrice = priceRepository.findActivePrice(
                testPrice.getProductId(),
                testPrice.getSellerId(),
                testPrice.getSiteId(),
                now
        );

        assertThat(foundPrice).isPresent();
        assertThat(foundPrice.get().getProductId()).isEqualTo(testPrice.getProductId());
    }

    @Test
    void findByProductId_ShouldReturnAllPricesForProduct() {
        List<Price> results = priceRepository.findByProductId(testPrice.getProductId());

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo(testPrice.getProductId());
    }

    @Test
    void findBySellerIdAndSiteId_ShouldReturnAllPricesForSellerAndSite() {
        List<Price> results = priceRepository.findBySellerIdAndSiteId(
                testPrice.getSellerId(),
                testPrice.getSiteId()
        );
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getSellerId()).isEqualTo(testPrice.getSellerId());
        assertThat(results.get(0).getSiteId()).isEqualTo(testPrice.getSiteId());
    }

    @Test
    void findUpcomingPriceChanges_ShouldReturnFuturePrices() {
        // Create a future price
        Price futurePrice = Price.builder()
                .productId(2L)
                .sellerId(testPrice.getSellerId())
                .siteId(testPrice.getSiteId())
                .basePrice(new BigDecimal("200.00"))
                .sellingPrice(new BigDecimal("180.00"))
                .currency("USD")
                .effectiveFrom(now.plusDays(5))
                .effectiveTo(now.plusDays(35))
                .isActive(true)
                .build();
        priceRepository.save(futurePrice);

        List<Price> results = priceRepository.findUpcomingPriceChanges(
                testPrice.getSellerId(),
                testPrice.getSiteId(),
                now.plusDays(1),
                now.plusDays(30)
        );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getProductId()).isEqualTo(futurePrice.getProductId());
    }

    @Test
    void findByProductIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThan_ShouldReturnValidPrice() {
        Optional<Price> result = priceRepository
                .findByProductIdAndEffectiveFromLessThanEqualAndEffectiveToGreaterThan(
                        testPrice.getProductId(),
                        now.plusDays(1),
                        now.plusDays(1)
                );

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(testPrice.getProductId());
    }

    @Test
    void basicCrudOperations() {
        // Create
        Price newPrice = Price.builder()
                .productId(3L)
                .sellerId(2L)
                .siteId(2L)
                .basePrice(new BigDecimal("150.00"))
                .sellingPrice(new BigDecimal("140.00"))
                .currency("USD")
                .effectiveFrom(now)
                .effectiveTo(now.plusDays(30))
                .isActive(true)
                .build();

        Price savedPrice = priceRepository.save(newPrice);
        assertThat(savedPrice.getId()).isNotNull();

        // Read
        Optional<Price> foundPrice = priceRepository.findById(savedPrice.getId());
        assertThat(foundPrice).isPresent();
        assertThat(foundPrice.get().getProductId()).isEqualTo(newPrice.getProductId());

        // Update
        savedPrice.setSellingPrice(new BigDecimal("145.00"));
        Price updatedPrice = priceRepository.save(savedPrice);
        assertThat(updatedPrice.getSellingPrice()).isEqualTo(new BigDecimal("145.00"));

        // Delete
        priceRepository.deleteById(savedPrice.getId());
        assertThat(priceRepository.findById(savedPrice.getId())).isEmpty();
    }
}