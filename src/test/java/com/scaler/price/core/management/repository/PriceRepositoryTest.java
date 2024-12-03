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
        testPrice = new Price();
        testPrice.setProductId(1L);
        testPrice.setSellerId(1L);
        testPrice.setSiteId(1L);
        testPrice.setBasePrice(new BigDecimal("100.00"));
        testPrice.setSellingPrice(new BigDecimal("90.00"));
        testPrice.setEffectiveFrom(now);
        testPrice.setEffectiveTo(now.plusDays(30));
        testPrice.setIsActive(true);
        testPrice = priceRepository.save(testPrice);
    }

    @Test
    void findActivePrice_WhenPriceExists_ShouldReturnPrice() {
        Optional<Price> result = priceRepository.findActivePrice(
                testPrice.getProductId(),
                testPrice.getSellerId(),
                testPrice.getSiteId(),
                now.plusDays(1)
        );

        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(testPrice.getProductId());
        assertThat(result.get().getSellingPrice()).isEqualTo(testPrice.getSellingPrice());
    }

    @Test
    void findActivePrice_WhenPriceDoesNotExist_ShouldReturnEmpty() {
        Optional<Price> result = priceRepository.findActivePrice(999L, 999L, 999L, now);
        assertThat(result).isEmpty();
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
        Price futurePrice = new Price();
        futurePrice.setProductId(2L);
        futurePrice.setSellerId(testPrice.getSellerId());
        futurePrice.setSiteId(testPrice.getSiteId());
        futurePrice.setBasePrice(new BigDecimal("200.00"));
        futurePrice.setSellingPrice(new BigDecimal("180.00"));
        futurePrice.setEffectiveFrom(now.plusDays(5));
        futurePrice.setEffectiveTo(now.plusDays(35));
        futurePrice.setIsActive(true);
        priceRepository.save(futurePrice);

        List<Price> results = priceRepository.findUpcomingPriceChanges(
                testPrice.getSellerId(),
                testPrice.getSiteId(),
                now,
                now.plusDays(10)
        );

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Price::getEffectiveFrom)
                .allMatch(date -> date.isAfter(now.minusDays(1)) && date.isBefore(now.plusDays(11)));
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
        Price newPrice = new Price();
        newPrice.setProductId(3L);
        newPrice.setSellerId(2L);
        newPrice.setSiteId(2L);
        newPrice.setBasePrice(new BigDecimal("150.00"));
        newPrice.setSellingPrice(new BigDecimal("140.00"));
        newPrice.setEffectiveFrom(now);
        newPrice.setEffectiveTo(now.plusDays(30));
        newPrice.setIsActive(true);

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