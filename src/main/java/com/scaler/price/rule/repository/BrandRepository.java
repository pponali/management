package com.scaler.price.rule.repository;


import com.scaler.price.rule.domain.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long>, JpaSpecificationExecutor<Brand> {

    /**
     * Find a brand by its unique name
     * @param name The brand name to search for
     * @return Optional containing the brand if found
     */
    Optional<Brand> findByNameIgnoreCase(String name);

    /**
     * Find all active brands ordered by name
     * @return List of active brands
     */
    List<Brand> findByActiveIsTrueOrderByNameAsc();

    /**
     * Search brands by name pattern
     * @param namePattern The pattern to search for
     * @param pageable Pagination information
     * @return Page of matching brands
     */
    Page<Brand> findByNameContainingIgnoreCase(String namePattern, Pageable pageable);

    /**
     * Find brands by category ID
     * @param categoryId The category ID to search for
     * @return List of brands in the category
     */
    @Query("SELECT b FROM Brand b JOIN b.categories c WHERE c.id = :categoryId AND b.active = true")
    List<Brand> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * Check if a brand name already exists (case insensitive)
     * @param name The brand name to check
     * @return true if the brand name exists
     */
    boolean existsByNameIgnoreCase(String name);

    /**
     * Find brands created after a specific brand ID, ordered by creation date
     * @param brandId The reference brand ID
     * @return List of brands
     */
    @Query(value = """
            SELECT b FROM Brand b 
            WHERE b.id > :brandId AND b.active = true 
            ORDER BY b.createdAt DESC
            """)
    List<Brand> findRecentBrandsAfter(@Param("brandId") Long brandId, Pageable pageable);

    /**
     * Count active brands in a specific category
     * @param categoryId The category ID
     * @return Number of active brands in the category
     */
    @Query("SELECT COUNT(b) FROM Brand b JOIN b.categories c WHERE c.id = :categoryId AND b.active = true")
    long countActiveBrandsByCategory(@Param("categoryId") Long categoryId);

    /**
     * Find popular brands based on product count
     * @param limit Maximum number of results
     * @return List of brands with their product counts
     */
    @Query(value = """
            SELECT b.*, COUNT(p.id) as productCount 
            FROM brands b 
            LEFT JOIN products p ON p.brand_id = b.id 
            WHERE b.active = true 
            GROUP BY b.id 
            ORDER BY productCount DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<Brand> findPopularBrands(@Param("limit") int limit);

    /**
     * Soft delete a brand by ID
     * @param brandId The brand ID to delete
     * @return Number of affected rows
     */
    @Query("UPDATE Brand b SET b.active = false WHERE b.id = :brandId")
    int softDeleteById(@Param("brandId") Long brandId);

    /**
     * Find brands with low inventory products
     * @param threshold The inventory threshold
     * @return List of brands
     */
    @Query("""
            SELECT DISTINCT b FROM Brand b 
            JOIN b.products p 
            WHERE b.active = true 
            AND p.stockQuantity < :threshold
            """)
    List<Brand> findBrandsWithLowInventory(@Param("threshold") int threshold);
}