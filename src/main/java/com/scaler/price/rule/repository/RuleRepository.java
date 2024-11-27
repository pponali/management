package com.scaler.price.rule.repository;

import com.scaler.price.core.management.dto.ConflictSummary;
import com.scaler.price.rule.domain.*;
import com.scaler.price.rule.dto.RuleSiteSummary;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public interface RuleRepository extends JpaRepository<PricingRule, Long> {

    @Query("""
        SELECT r FROM Rule r
        WHERE r.sellerId = :sellerId
        AND r.isActive = true
        AND r.effectiveFrom <= :currentDate
        AND (r.effectiveTo IS NULL OR r.effectiveTo >= :currentDate)
        ORDER BY r.priority DESC
        """)
    List<PricingRule> findActiveRules(
            @Param("sellerId") String sellerId,
            @Param("currentDate") LocalDateTime currentDate
    );

    @Query("""
        SELECT r FROM Rule r
        WHERE r.sellerId = :sellerId
        AND r.siteId = :siteId
        AND (:categoryId IS NULL OR r.categoryId = :categoryId)
        AND (:brandId IS NULL OR r.brandId = :brandId)
        AND r.isActive = true
        AND r.effectiveFrom <= :currentDate
        AND (r.effectiveTo IS NULL OR r.effectiveTo >= :currentDate)
        ORDER BY r.priority DESC
        """)
    List<PricingRule> findApplicableRules(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("categoryId") String categoryId,
            @Param("brandId") String brandId,
            @Param("currentDate") LocalDateTime currentDate
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN pr.sellerIds s
        LEFT JOIN pr.siteIds site
        WHERE pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        AND s = :sellerId
        AND (site IN :siteIds)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findActiveRulesForSeller(
            @Param("sellerId") String sellerId,
            @Param("siteIds") Set<String> siteIds,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN pr.sellerIds s
        LEFT JOIN pr.siteIds site
        WHERE pr.isActive = true
        AND s = :sellerId
        AND site = :siteId
        AND pr.ruleType = :ruleType
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findActiveRulesForSellerAndSite(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("ruleType") RuleType ruleType,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerSiteConfigs config
        WHERE pr.isActive = true
        AND config.sellerId = :sellerId
        AND config.siteId = :siteId
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        AND (
            config.minimumPrice <= :price
            AND (config.maximumPrice IS NULL OR config.maximumPrice >= :price)
        )
        ORDER BY pr.priority DESC, config.priority DESC
        """)
    List<PricingRule> findApplicableRulesForPrice(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("price") BigDecimal price,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerIds s
        WHERE s = :sellerId
        AND pr.effectiveFrom >= :startDate
        AND pr.effectiveFrom <= :endDate
        """)
    List<PricingRule> findUpcomingRulesForSeller(
            @Param("sellerId") String sellerId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("""
        SELECT pr FROM PricingRule pr
        JOIN pr.sellerSiteConfigs config
        WHERE config.sellerId = :sellerId
        AND config.siteId = :siteId
        AND pr.isActive = true
        AND pr.ruleType IN :ruleTypes
        AND EXISTS (
            SELECT 1 FROM pr.conditions c
            WHERE c.attributeKey = :attributeKey
        )
        """)
    List<PricingRule> findRulesBySellerAndAttribute(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("ruleTypes") Set<RuleType> ruleTypes,
            @Param("attributeKey") String attributeKey
    );

    /*@Query("""
        SELECT new com.scaler.price.rules.dto.SellerRuleSummary(
            s,
            COUNT(pr),
            COUNT(CASE WHEN pr.isActive = true THEN 1 END),
            MIN(pr.effectiveFrom),
            MAX(pr.effectiveTo)
        )
        FROM PricingRule pr
        JOIN pr.sellerIds s
        WHERE s IN :sellerIds
        GROUP BY s
        """)
    List<SellerRuleSummary> getSellerRuleSummary(
            @Param("sellerIds") Set<String> sellerIds
    );*/

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerSiteConfigs config
        WHERE config.sellerId = :sellerId
        AND config.siteId = :siteId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        AND (
            config.minimumMargin IS NOT NULL
            OR config.maximumMargin IS NOT NULL
        )
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findMarginRulesForSeller(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Modifying
    @Query("""
        UPDATE PricingRule pr
        SET pr.isActive = false,
           pr.modifiedAt = :modifiedAt,
           pr.modifiedBy = :modifiedBy
        WHERE pr.id IN (
            SELECT r.id FROM PricingRule r
            JOIN r.sellerIds s
            WHERE s = :sellerId
        )
        """)
    int deactivateSellerRules(
            @Param("sellerId") String sellerId,
            @Param("modifiedAt") LocalDateTime modifiedAt,
            @Param("modifiedBy") String modifiedBy
    );

    @Query(value = """
        SELECT DISTINCT category_id
        FROM rule_category_mappings rcm
        JOIN pricing_rules pr ON rcm.rule_id = pr.id
        JOIN rule_seller_mappings rsm ON pr.id = rsm.rule_id
        WHERE rsm.seller_id = :sellerId
        AND pr.is_active = true
        """, nativeQuery = true)
    List<String> findActiveCategoriesForSeller(
            @Param("sellerId") String sellerId
    );

    @Query("""
        SELECT new com.scaler.price.rule.dto.RuleConflict(
            pr1.id,
            pr2.id,
            pr1.ruleName,
            pr2.ruleName,
            'PRIORITY_CONFLICT'
        )
        FROM PricingRule pr1, PricingRule pr2
        WHERE pr1.id < pr2.id
        AND pr1.isActive = true AND pr2.isActive = true
        AND EXISTS (
            SELECT 1 FROM pr1.sellerIds s1, pr2.sellerIds s2
            WHERE s1 = s2 AND s1 = :sellerId
        )
        AND EXISTS (
            SELECT 1 FROM pr1.siteIds site1, pr2.siteIds site2
            WHERE site1 = site2
        )
        AND pr1.priority = pr2.priority
        AND pr1.ruleType = pr2.ruleType
        """)
    List<RuleConflict> findSellerRuleConflicts(
            @Param("sellerId") String sellerId
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerIds s
        JOIN pr.siteIds site
        WHERE pr.isActive = true
        AND (
            :effectiveFrom BETWEEN pr.effectiveFrom AND pr.effectiveTo
            OR :effectiveTo BETWEEN pr.effectiveFrom AND pr.effectiveTo
            OR (pr.effectiveFrom BETWEEN :effectiveFrom AND :effectiveTo)
        )
        AND s IN :sellerIds
        AND site IN :siteIds
        AND pr.priority IS NOT NULL
        """)
    List<PricingRule> findConflictingRules(
            @Param("sellerIds") Set<String> sellerIds,
            @Param("siteIds") Set<String> siteIds,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo
    );

    // Additional conflict queries for specific scenarios
    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerIds s
        JOIN pr.siteIds site
        WHERE pr.isActive = true
        AND s IN :sellerIds
        AND site IN :siteIds
        AND pr.ruleType = :ruleType
        AND pr.priority = :priority
        AND (
            :effectiveFrom BETWEEN pr.effectiveFrom AND pr.effectiveTo
            OR :effectiveTo BETWEEN pr.effectiveFrom AND pr.effectiveTo
        )
        """)
    List<PricingRule> findPriorityConflicts(
            @Param("sellerIds") Set<String> sellerIds,
            @Param("siteIds") Set<String> siteIds,
            @Param("ruleType") RuleType ruleType,
            @Param("priority") Integer priority,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerIds s
        JOIN pr.siteIds site
        WHERE pr.isActive = true
        AND s IN :sellerIds
        AND site IN :siteIds
        AND pr.ruleType IN :ruleTypes
        AND (
            :effectiveFrom BETWEEN pr.effectiveFrom AND pr.effectiveTo
            OR :effectiveTo BETWEEN pr.effectiveFrom AND pr.effectiveTo
            OR (pr.effectiveFrom BETWEEN :effectiveFrom AND :effectiveTo)
        )
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findTypeBasedConflicts(
            @Param("sellerIds") Set<String> sellerIds,
            @Param("siteIds") Set<String> siteIds,
            @Param("ruleTypes") Set<RuleType> ruleTypes,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo
    );

    // Native SQL query example for complex conflict detection
    @Query(value = """
        WITH overlapping_rules AS (
            SELECT pr1.id as rule1_id, 
                   pr2.id as rule2_id,
                   pr1.rule_name as rule1_name,
                   pr2.rule_name as rule2_name,
                   pr1.priority as rule1_priority,
                   pr2.priority as rule2_priority
            FROM pricing_rules pr1
            CROSS JOIN pricing_rules pr2
            WHERE pr1.id < pr2.id
            AND pr1.is_active = true 
            AND pr2.is_active = true
            AND EXISTS (
                SELECT 1 
                FROM rule_seller_mappings sm1
                JOIN rule_seller_mappings sm2 
                ON sm1.seller_id = sm2.seller_id
                WHERE sm1.rule_id = pr1.id 
                AND sm2.rule_id = pr2.id
                AND sm1.seller_id IN :sellerIds
            )
            AND EXISTS (
                SELECT 1 
                FROM rule_site_mappings site1
                JOIN rule_site_mappings site2 
                ON site1.site_id = site2.site_id
                WHERE site1.rule_id = pr1.id 
                AND site2.rule_id = pr2.id
                AND site1.site_id IN :siteIds
            )
            AND (
                (pr1.effective_from <= :effectiveTo AND pr1.effective_to >= :effectiveFrom)
                OR (pr2.effective_from <= :effectiveTo AND pr2.effective_to >= :effectiveFrom)
            )
        )
        SELECT pr.* 
        FROM pricing_rules pr
        WHERE pr.id IN (
            SELECT rule1_id FROM overlapping_rules
            UNION
            SELECT rule2_id FROM overlapping_rules
        )
        """,
            nativeQuery = true)
    List<PricingRule> findDetailedConflicts(
            @Param("sellerIds") Set<String> sellerIds,
            @Param("siteIds") Set<String> siteIds,
            @Param("effectiveFrom") LocalDateTime effectiveFrom,
            @Param("effectiveTo") LocalDateTime effectiveTo
    );

    // Additional helper queries
    @Query("""
        SELECT COUNT(pr) FROM PricingRule pr
        JOIN pr.sellerIds s
        WHERE s = :sellerId
        AND pr.isActive = true
        """)
    long countActiveRulesForSeller(@Param("sellerId") String sellerId);

    @Query("""
        SELECT new com.scaler.price.rule.dto.ConflictSummary(
            pr1.id,
            pr2.id,
            pr1.ruleName,
            pr2.ruleName,
            CASE 
                WHEN pr1.priority = pr2.priority THEN 'PRIORITY_CONFLICT'
                WHEN pr1.ruleType = pr2.ruleType THEN 'TYPE_CONFLICT'
                ELSE 'DATE_OVERLAP'
            END
        )
        FROM PricingRule pr1, PricingRule pr2
        WHERE pr1.id < pr2.id
        AND pr1.isActive = true AND pr2.isActive = true
        AND EXISTS (
            SELECT 1 FROM pr1.sellerIds s1, pr2.sellerIds s2
            WHERE s1 = s2 AND s1 IN :sellerIds
        )
        AND EXISTS (
            SELECT 1 FROM pr1.siteIds site1, pr2.siteIds site2
            WHERE site1 = site2 AND site1 IN :siteIds
        )
        AND (
            pr1.effectiveFrom BETWEEN pr2.effectiveFrom AND pr2.effectiveTo
            OR pr1.effectiveTo BETWEEN pr2.effectiveFrom AND pr2.effectiveTo
        )
        """)
    List<ConflictSummary> findConflictSummaries(
            @Param("sellerIds") Set<String> sellerIds,
            @Param("siteIds") Set<String> siteIds
    );

    /**
     * Find all pricing rules that contain the specified seller ID in their sellerIds collection.
     * This query joins with the rule_seller_mappings table to check for seller ID matches.
     *
     * @param sellerId the seller ID to search for
     * @return list of pricing rules that apply to the given seller
     */
    @Query("""
        SELECT DISTINCT r FROM PricingRule r
        JOIN r.sellerIds s
        WHERE s = :sellerId
        AND r.isActive = true
        ORDER BY r.priority DESC
        """)
    List<PricingRule> findBySellerIdsContaining(@Param("sellerId") String sellerId);

    List<PricingRule> findAll(Specification<PricingRule> spec, Sort priority);

    @Data
    @AllArgsConstructor
    public static class SellerRuleSummary {
        private String sellerId;
        private Long totalRules;
        private Long activeRules;
        private LocalDateTime earliestRule;
        private LocalDateTime latestRule;
    }

    @Data
    @AllArgsConstructor
    public static class RuleConflict {
        private Long ruleId1;
        private Long ruleId2;
        private String ruleName1;
        private String ruleName2;
        private String conflictType;
    }

    // Specification-based queries for complex filtering
    default Specification<PricingRule> createSellerSpecification(
            String sellerId,
            Set<String> siteIds,
            Set<RuleType> ruleTypes,
            boolean activeOnly) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Seller check
            Join<PricingRule, String> sellerJoin = root.join("sellerIds");
            predicates.add(cb.equal(sellerJoin, sellerId));

            // Site check
            if (siteIds != null && !siteIds.isEmpty()) {
                Join<PricingRule, String> siteJoin = root.join("siteIds");
                predicates.add(siteJoin.in(siteIds));
            }

            // Rule type check
            if (ruleTypes != null && !ruleTypes.isEmpty()) {
                predicates.add(root.get("ruleType").in(ruleTypes));
            }

            // Active check
            if (activeOnly) {
                predicates.add(cb.equal(root.get("isActive"), true));
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("effectiveFrom"),
                        LocalDateTime.now()
                ));
                predicates.add(cb.or(
                        cb.isNull(root.get("effectiveTo")),
                        cb.greaterThanOrEqualTo(
                                root.get("effectiveTo"),
                                LocalDateTime.now()
                        )
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };


    }


    // Count methods using derived query names
    long countBySiteIdsContaining(String siteId);
    long countBySellerIdsContaining(String sellerId);

    // More specific count queries with @Query
    @Query("""
        SELECT COUNT(DISTINCT pr) FROM PricingRule pr
        JOIN pr.siteIds site
        WHERE site = :siteId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        """)
    long countActiveRulesBySite(
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT COUNT(DISTINCT pr) FROM PricingRule pr
        JOIN pr.sellerIds seller
        JOIN pr.siteIds site
        WHERE seller = :sellerId
        AND site = :siteId
        AND pr.isActive = true
        AND pr.ruleType = :ruleType
        """)
    long countActiveRulesBySellerAndSite(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("ruleType") RuleType ruleType
    );

    @Query("""
        SELECT new com.scaler.price.rule.dto.RuleCount(
            site,
            COUNT(DISTINCT pr),
            COUNT(DISTINCT CASE WHEN pr.isActive = true THEN pr.id ELSE null END)
        )
        FROM PricingRule pr
        JOIN pr.siteIds site
        GROUP BY site
        """)
    List<RuleCount> countRulesPerSite();

    @Query("""
        SELECT COUNT(DISTINCT pr) FROM PricingRule pr
        JOIN pr.siteIds site
        WHERE site = :siteId
        AND pr.isActive = true
        AND EXISTS (
            SELECT 1 FROM pr.conditions c
            WHERE c.type = :conditionType
        )
        """)
    long countRulesByConditionType(
            @Param("siteId") String siteId,
            @Param("conditionType") ConditionType conditionType
    );

    // Native query for complex counting
    @Query(value = """
        WITH rule_stats AS (
            SELECT s.site_id,
                   COUNT(DISTINCT pr.id) as total_rules,
                   COUNT(DISTINCT CASE WHEN pr.is_active = true THEN pr.id END) as active_rules,
                   COUNT(DISTINCT CASE WHEN pr.effective_to < CURRENT_TIMESTAMP THEN pr.id END) as expired_rules
            FROM pricing_rules pr
            JOIN rule_site_mappings s ON pr.id = s.rule_id
            WHERE s.site_id = :siteId
            GROUP BY s.site_id
        )
        SELECT 
            site_id,
            total_rules,
            active_rules,
            expired_rules,
            CASE 
                WHEN total_rules > 0 THEN (active_rules::float / total_rules) * 100 
                ELSE 0 
            END as active_percentage
        FROM rule_stats
        """,
            nativeQuery = true)
    List<RuleStatistics> getRuleStatistics(@Param("siteId") String siteId);

    // DTOs for count results
    @Data
    @AllArgsConstructor
    public static class RuleCount {
        private String siteId;
        private Long totalCount;
        private Long activeCount;
    }

    public interface RuleStatistics {
        String getSiteId();
        Long getTotalRules();
        Long getActiveRules();
        Long getExpiredRules();
        Double getActivePercentage();
    }

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.siteIds s
        LEFT JOIN FETCH pr.sellerSiteConfigs ssc
        LEFT JOIN FETCH pr.conditions c
        LEFT JOIN FETCH pr.actions a
        WHERE pr.id = :ruleId 
        AND s = :siteId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        """)
    Optional<PricingRule> findBySiteAndId(
            @Param("ruleId") Long ruleId,
            @Param("siteId") String siteId
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.siteIds s
        WHERE s = :siteId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findActiveBySite(
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.siteIds s
        JOIN pr.sellerIds sel
        WHERE s = :siteId
        AND sel = :sellerId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findActiveBySellerAndSite(
            @Param("sellerId") String sellerId,
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.siteIds s
        WHERE s = :siteId
        AND pr.ruleType = :ruleType
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        ORDER BY pr.priority DESC
        """)
    List<PricingRule> findActiveByTypeAndSite(
            @Param("ruleType") RuleType ruleType,
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query(value = """
        WITH RECURSIVE rule_hierarchy AS (
            SELECT r.*, 1 as level
            FROM pricing_rules r
            JOIN rule_site_mappings sm ON r.id = sm.rule_id
            WHERE sm.site_id = :siteId
            AND r.parent_rule_id IS NULL
            
            UNION ALL
            
            SELECT r.*, rh.level + 1
            FROM pricing_rules r
            JOIN rule_hierarchy rh ON r.parent_rule_id = rh.id
            WHERE r.is_active = true
        )
        SELECT * FROM rule_hierarchy
        WHERE level <= :maxLevel
        ORDER BY level, priority DESC
        """, nativeQuery = true)
    List<PricingRule> findRuleHierarchyBySite(
            @Param("siteId") String siteId,
            @Param("maxLevel") Integer maxLevel
    );

    @Query("""
        SELECT COUNT(pr) FROM PricingRule pr
        JOIN pr.siteIds s
        WHERE s = :siteId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        """)
    long countActiveBySite(
            @Param("siteId") String siteId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT new com.scaler.price.rule.dto.RuleSiteSummary(
            s,
            COUNT(pr),
            COUNT(CASE WHEN pr.isActive = true THEN 1 END),
            MIN(pr.effectiveFrom),
            MAX(pr.effectiveTo)
        )
        FROM PricingRule pr
        JOIN pr.siteIds s
        GROUP BY s
        """)
    List<RuleSiteSummary> getRuleSummariesBySite();

    // Specifications for dynamic queries
    default Specification<PricingRule> siteSpec(String siteId) {
        return (root, query, cb) -> {
            Join<PricingRule, String> siteJoin = root.join("siteIds");
            return cb.equal(siteJoin, siteId);
        };
    }

    default Specification<PricingRule> activeSpec() {
        return (root, query, cb) -> {
            LocalDateTime now = LocalDateTime.now();
            return cb.and(
                    cb.equal(root.get("isActive"), true),
                    cb.lessThanOrEqualTo(root.get("effectiveFrom"), now),
                    cb.or(
                            cb.isNull(root.get("effectiveTo")),
                            cb.greaterThanOrEqualTo(root.get("effectiveTo"), now)
                    )
            );
        };
    }

    default Specification<PricingRule> typeSpec(RuleType ruleType) {
        return (root, query, cb) ->
                cb.equal(root.get("ruleType"), ruleType);
    }

    default Specification<PricingRule> prioritySpec(Integer minPriority) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("priority"), minPriority);
    }

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN FETCH pr.sellerSiteConfigs ssc
        LEFT JOIN FETCH pr.conditions c
        LEFT JOIN FETCH pr.actions a
        LEFT JOIN FETCH pr.sellerIds
        LEFT JOIN FETCH pr.siteIds
        WHERE pr.id = :ruleId
        """)
    Optional<PricingRule> findRuleWithDetails(@Param("ruleId") Long ruleId);

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        JOIN pr.sellerIds s
        LEFT JOIN FETCH pr.sellerSiteConfigs ssc
        LEFT JOIN FETCH pr.conditions c
        LEFT JOIN FETCH pr.actions a
        WHERE pr.id = :ruleId
        AND s = :sellerId
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        """)
    Optional<PricingRule> findBySellerAndId(
            @Param("ruleId") Long ruleId,
            @Param("sellerId") String sellerId,
            @Param("currentTime") LocalDateTime currentTime
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN FETCH pr.sellerSiteConfigs ssc
        LEFT JOIN FETCH pr.conditions c
        LEFT JOIN FETCH pr.actions a
        WHERE pr.id = :ruleId
        AND pr.status = :status
        AND pr.isActive = true
        AND pr.effectiveFrom <= :currentTime
        AND (pr.effectiveTo IS NULL OR pr.effectiveTo >= :currentTime)
        """)
    Optional<PricingRule> findByIdAndStatus(
            @Param("ruleId") Long ruleId,
            @Param("status") RuleStatus status,
            @Param("currentTime") LocalDateTime currentTime
    );

    // Additional helper queries for related data
    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN FETCH pr.sellerSiteConfigs ssc
        WHERE pr.id IN :ruleIds
        """)
    List<PricingRule> findRulesWithConfigs(
            @Param("ruleIds") Collection<Long> ruleIds
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN FETCH pr.conditions c
        WHERE pr.id IN :ruleIds
        """)
    List<PricingRule> findRulesWithConditions(
            @Param("ruleIds") Collection<Long> ruleIds
    );

    @Query("""
        SELECT DISTINCT pr FROM PricingRule pr
        LEFT JOIN FETCH pr.actions a
        WHERE pr.id IN :ruleIds
        """)
    List<PricingRule> findRulesWithActions(
            @Param("ruleIds") Collection<Long> ruleIds
    );

    // Service methods using these queries
    @Transactional(readOnly = true)
    default List<PricingRule> findDetailedRules(Collection<Long> ruleIds) {
        // Load rules with all related data in separate queries to avoid cartesian product
        Map<Long, PricingRule> rulesWithConfigs = findRulesWithConfigs(ruleIds)
                .stream()
                .collect(Collectors.toMap(PricingRule::getId, Function.identity()));

        Map<Long, List<RuleCondition>> conditions = findRulesWithConditions(ruleIds)
                .stream()
                .collect(Collectors.toMap(
                        PricingRule::getId,
                        rule -> new ArrayList<>(rule.getConditions())
                ));

        Map<Long, List<RuleAction>> actions = findRulesWithActions(ruleIds)
                .stream()
                .collect(Collectors.toMap(
                        PricingRule::getId,
                        rule -> new ArrayList<>(rule.getActions())
                ));

        // Combine the data
        rulesWithConfigs.forEach((ruleId, rule) -> {
            rule.setConditions(new HashSet<>(conditions.getOrDefault(
                    ruleId, Collections.emptyList()))
            );
            rule.setActions(new HashSet<>(actions.getOrDefault(
                    ruleId, Collections.emptyList()))
            );
        });

        return new ArrayList<>(rulesWithConfigs.values());
    }

    List<PricingRule> findRulesBySite(String siteIdStr);
}
