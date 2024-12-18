package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.Brand;
import com.scaler.price.rule.domain.constraint.CategoryConstraints;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class BrandRepositoryTest {

    @Autowired
    private BrandRepository brandRepository;

    private Brand testBrand;
    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        testBrand = Brand.builder()
                .name("Test Brand")
                .description("Test Description")
                .logoUrl("http://test-logo.com")
                .active(true)
                .contactEmail("test@brand.com")
                .contactPhone("+1234567890")
                .websiteUrl("http://testbrand.com")
                .categories(new HashSet<>())
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    @Test
    void shouldSaveAndRetrieveBrand() {
        // when
        Brand savedBrand = brandRepository.save(testBrand);

        // then
        assertThat(savedBrand).isNotNull();
        assertThat(savedBrand.getId()).isNotNull();
        assertThat(savedBrand.getName()).isEqualTo(testBrand.getName());
        assertThat(savedBrand.getDescription()).isEqualTo(testBrand.getDescription());
        assertThat(savedBrand.getLogoUrl()).isEqualTo(testBrand.getLogoUrl());
        assertThat(savedBrand.isActive()).isTrue();
    }

    @Test
    void shouldFindBrandById() {
        // given
        Brand savedBrand = brandRepository.save(testBrand);

        // when
        Optional<Brand> foundBrand = brandRepository.findById(savedBrand.getId());

        // then
        assertThat(foundBrand).isPresent();
        assertThat(foundBrand.get().getName()).isEqualTo(testBrand.getName());
    }

    @Test
    void shouldFindBrandsByCategory() {
        // given
        CategoryConstraints category = CategoryConstraints.builder()
                .categoryId(1L)
                .build();
        Set<CategoryConstraints> categories = new HashSet<>();
        categories.add(category);
        testBrand.setCategories(categories);
        brandRepository.save(testBrand);

        // when
        List<Brand> brands = brandRepository.findByCategoryId(1L);

        // then
        assertThat(brands).hasSize(1);
        assertThat(brands.get(0).getName()).isEqualTo(testBrand.getName());
    }

    @Test
    void shouldFindByNameContainingIgnoreCase() {
        // given
        brandRepository.save(testBrand);

        // when
        Page<Brand> brands = brandRepository.findByNameContainingIgnoreCase("test", PageRequest.of(0, 10));

        // then
        assertThat(brands).isNotEmpty();
        assertThat(brands.getContent().get(0).getName()).isEqualTo(testBrand.getName());
    }

    @Test
    void shouldCheckExistsByNameIgnoreCase() {
        // given
        brandRepository.save(testBrand);

        // when
        boolean exists = brandRepository.existsByNameIgnoreCase("TEST BRAND");

        // then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldUpdateBrand() {
        // given
        Brand savedBrand = brandRepository.save(testBrand);
        
        // when
        savedBrand.setName("Updated Brand Name");
        Brand updatedBrand = brandRepository.save(savedBrand);

        // then
        assertThat(updatedBrand.getName()).isEqualTo("Updated Brand Name");
        assertThat(updatedBrand.getId()).isEqualTo(savedBrand.getId());
    }

    @Test
    void shouldDeleteBrand() {
        // given
        Brand savedBrand = brandRepository.save(testBrand);

        // when
        brandRepository.deleteById(savedBrand.getId());
        Optional<Brand> deletedBrand = brandRepository.findById(savedBrand.getId());

        // then
        assertThat(deletedBrand).isEmpty();
    }

    @Test
    void shouldThrowExceptionWhenSavingBrandWithNullName() {
        // given
        testBrand.setName(null);

        // when/then
        assertThatThrownBy(() -> brandRepository.save(testBrand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not-null property references a null or transient value");
    }

    @Test
    void shouldThrowExceptionWhenSavingBrandWithNullLogoUrl() {
        // given
        testBrand.setLogoUrl(null);

        // when/then
        assertThatThrownBy(() -> brandRepository.save(testBrand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not-null property references a null or transient value");
    }

    @Test
    void shouldHandleDuplicateBrandName() {
        // given
        brandRepository.save(testBrand);
        Brand duplicateBrand = Brand.builder()
                .name(testBrand.getName())
                .description("Another description")
                .logoUrl("http://another-logo.com")
                .active(true)
                .build();

        // when/then
        assertThatThrownBy(() -> brandRepository.save(duplicateBrand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("constraint violation");
    }

    @Test
    void shouldReturnEmptyPageWhenSearchingNonExistentBrandName() {
        // given
        brandRepository.save(testBrand);

        // when
        Page<Brand> result = brandRepository.findByNameContainingIgnoreCase("nonexistent", PageRequest.of(0, 10));

        // then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void shouldReturnEmptyListWhenFindingByNonExistentCategoryId() {
        // given
        CategoryConstraints category = CategoryConstraints.builder()
                .categoryId(1L)
                .build();
        testBrand.setCategories(Set.of(category));
        brandRepository.save(testBrand);

        // when
        List<Brand> result = brandRepository.findByCategoryId(999L);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldHandleEmptyCategorySet() {
        // given
        testBrand.setCategories(new HashSet<>());
        Brand savedBrand = brandRepository.save(testBrand);

        // when
        Brand retrievedBrand = brandRepository.findById(savedBrand.getId()).orElseThrow();

        // then
        assertThat(retrievedBrand.getCategories()).isEmpty();
    }

    @Test
    void shouldHandleMaxLengthDescription() {
        // given
        String maxLengthDescription = "A".repeat(500); // Max length as per @Column(length = 500)
        testBrand.setDescription(maxLengthDescription);

        // when
        Brand savedBrand = brandRepository.save(testBrand);

        // then
        assertThat(savedBrand.getDescription()).hasSize(500);
        assertThat(savedBrand.getDescription()).isEqualTo(maxLengthDescription);
    }

    @Test
    void shouldThrowExceptionWhenDescriptionExceedsMaxLength() {
        // given
        String tooLongDescription = "A".repeat(501); // Exceeds max length
        testBrand.setDescription(tooLongDescription);

        // when/then
        assertThatThrownBy(() -> brandRepository.save(testBrand))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("value too long");
    }

    @Test
    void shouldHandleSpecialCharactersInBrandName() {
        // given
        testBrand.setName("Brand#$@!&*()");

        // when
        Brand savedBrand = brandRepository.save(testBrand);

        // then
        assertThat(savedBrand.getName()).isEqualTo("Brand#$@!&*()");
    }

    @Test
    void shouldHandleInvalidEmailFormat() {
        // given
        testBrand.setContactEmail("invalid-email");

        // when
        Brand savedBrand = brandRepository.save(testBrand);

        // then
        assertThat(savedBrand.getContactEmail()).isEqualTo("invalid-email");
        // Note: Email validation should be handled at service layer, not repository
    }

    @Test
    void shouldHandleLongUrlValues() {
        // given
        String longUrl = "https://" + "a".repeat(500) + ".com";
        testBrand.setWebsiteUrl(longUrl);
        testBrand.setLogoUrl(longUrl);

        // when
        Brand savedBrand = brandRepository.save(testBrand);

        // then
        assertThat(savedBrand.getWebsiteUrl()).isEqualTo(longUrl);
        assertThat(savedBrand.getLogoUrl()).isEqualTo(longUrl);
    }
}
