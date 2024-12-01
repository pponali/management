package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.domain.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("managementConfigurationRepository")
public interface ConfigurationRepository extends JpaRepository<Configuration, Long> {

    Optional<Configuration> findByKeyAndSiteId(String key, String siteId);

    Optional<Configuration> findByKeyAndSiteIdAndIsActive(
            String key,
            String siteId,
            Boolean isActive
    );

    List<Configuration> findBySiteId(String siteId);

    List<Configuration> findByType(Configuration.ConfigType type);

    @Query("""
        SELECT c FROM Configuration c
        WHERE c.isActive = true
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        ORDER BY c.siteId NULLS LAST
        """)
    List<Configuration> findActiveConfigurations(@Param("siteId") Long siteId);

    @Query("""
        SELECT c FROM Configuration c
        WHERE c.key LIKE :keyPattern
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        AND c.isActive = true
        ORDER BY c.key
        """)
    List<Configuration> findByKeyPattern(
            @Param("keyPattern") String keyPattern,
            @Param("siteId") Long siteId
    );

    @Query("""
        SELECT DISTINCT c FROM Configuration c
        WHERE c.isActive = true
        AND c.type = :type
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        AND c.metadata IS NOT NULL
        ORDER BY c.key
        """)
    List<Configuration> findConfigurationsWithMetadata(
            @Param("type") Configuration.ConfigType type,
            @Param("siteId") Long siteId
    );

    @Modifying
    @Query("""
        UPDATE Configuration c
        SET c.value = :value, c.version = c.version + 1
        WHERE c.key = :key
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        AND c.isMutable = true
        """)
    int updateConfigurationValue(
            @Param("key") String key,
            @Param("value") String value,
            @Param("siteId") String siteId
    );

    @Query(value = """
        SELECT c.* FROM configurations c
        WHERE c.is_active = true
        AND (c.site_id = :siteId OR c.site_id IS NULL)
        AND c.metadata @> :metadata::jsonb
        """, nativeQuery = true)
    List<Configuration> findByMetadata(
            @Param("metadata") String metadata,
            @Param("siteId") Long siteId
    );

    Optional<Configuration> findByKey(String key);


    @Query("""
        SELECT c FROM Configuration c
        WHERE c.isActive = true
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        AND c.key = :key
        ORDER BY c.siteId DESC
        LIMIT 1
    """)
    Optional<Configuration> findActiveConfigurationByKeyAndSite(
            @Param("key") String key,
            @Param("siteId") Long siteId
    );

    @Query("""
        SELECT c FROM Configuration c
        WHERE c.isActive = true
        AND c.type = :type
        AND (c.siteId = :siteId OR c.siteId IS NULL)
        ORDER BY c.siteId DESC
    """)
    List<Configuration> findActiveConfigurationsByTypeAndSite(
            @Param("type") Configuration.ConfigType type,
            @Param("siteId") Long siteId
    );

    @Modifying
    @Query("UPDATE Configuration c SET c.isActive = false WHERE c.id = :id")
    void deactivateConfiguration(@Param("id") Long id);

    @Query("""
        SELECT COUNT(c) FROM Configuration c
        WHERE c.isActive = true
        AND c.type = :type
        AND (c.siteId = :siteId OR c.siteId IS NULL)
    """)
    long countActiveConfigurationsByTypeAndSite(
            @Param("type") Configuration.ConfigType type,
            @Param("siteId") Long siteId
    );
}
