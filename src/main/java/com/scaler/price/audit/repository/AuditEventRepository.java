package com.scaler.price.audit.repository;

import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.audit.domain.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEntry, Long> {

    /**
     * Find audit events by user ID
     * @param userId The ID of the user whose audit events are to be retrieved
     * @return List of audit events associated with the specified user
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.userId = :userId ORDER BY ae.eventTime DESC")
    List<AuditEntry> findByUserId(@Param("userId") String userId);

    /**
     * Find audit events by type
     * @param type The type of audit events to retrieve
     * @return List of audit events of the specified type
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.type = :type ORDER BY ae.eventTime DESC")
    List<AuditEntry> findByType(@Param("type") AuditEventType type);

    /**
     * Find audit events within a time range
     */
    List<AuditEntry> findByEventTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find audit events by user ID and type
     */
    List<AuditEntry> findByUserIdAndType(String userId, AuditEventType type);

    /**
     * Find audit events by user ID within a time range
     */
    List<AuditEntry> findByUserIdAndEventTimeBetween(String userId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find audit events by type within a time range
     */
    List<AuditEntry> findByTypeAndEventTimeBetween(AuditEventType type, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find paginated audit events
     */
    Page<AuditEntry> findAll(Pageable pageable);

    /**
     * Find paginated audit events by user ID
     */
    Page<AuditEntry> findByUserId(String userId, Pageable pageable);

    /**
     * Find paginated audit events by type
     */
    Page<AuditEntry> findByType(AuditEventType type, Pageable pageable);

    /**
     * Search audit events by event data content
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.data LIKE %:searchTerm%")
    List<AuditEntry> searchByEventData(@Param("searchTerm") String searchTerm);

    /**
     * Find latest audit event by rule ID
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.data LIKE %:ruleId% ORDER BY ae.eventTime DESC LIMIT 1")
    Optional<AuditEntry> findLatestByRuleId(@Param("ruleId") String ruleId);

    /**
     * Find all audit events for a specific rule
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.data LIKE %:ruleId% ORDER BY ae.eventTime DESC")
    List<AuditEntry> findAllByRuleId(@Param("ruleId") String ruleId);

    /**
     * Count events by type within a time range
     */
    long countByTypeAndEventTimeBetween(AuditEventType type, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * Find events by multiple types
     */
    List<AuditEntry> findByTypeIn(List<AuditEventType> types);

    /**
     * Find events by user ID and multiple types
     */
    List<AuditEntry> findByUserIdAndTypeIn(String userId, List<AuditEventType> types);

    /**
     * Find events newer than a specific timestamp
     */
    List<AuditEntry> findByEventTimeAfter(LocalDateTime timestamp);

    /**
     * Find events older than a specific timestamp
     */
    List<AuditEntry> findByEventTimeBefore(LocalDateTime timestamp);

    /**
     * Custom query to find events with specific rule modifications
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.type = 'RULE_MODIFIED' " +
            "AND ae.data LIKE %:fieldName% AND ae.data LIKE %:fieldValue%")
    List<AuditEntry> findRuleModificationsByField(@Param("fieldName") String fieldName,
                                                  @Param("fieldValue") String fieldValue);

    /**
     * Find events by user ID and type with pagination
     */
    Page<AuditEntry> findByUserIdAndType(String userId, AuditEventType type, Pageable pageable);

    /**
     * Custom query to get event statistics
     */
    @Query("SELECT ae.type, COUNT(ae) FROM AuditEntry ae " +
            "WHERE ae.eventTime BETWEEN :startTime AND :endTime " +
            "GROUP BY ae.type")
    List<Object[]> getEventStatistics(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * Find distinct users who performed specific event type
     */
    @Query("SELECT DISTINCT ae.userId FROM AuditEntry ae WHERE ae.type = :type")
    List<String> findDistinctUsersByType(@Param("type") AuditEventType type);

    /**
     * Delete old audit events
     */
    void deleteByEventTimeBefore(LocalDateTime timestamp);

    /**
     * Find events by complex criteria
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE " +
            "(:userId IS NULL OR ae.userId = :userId) AND " +
            "(:type IS NULL OR ae.type = :type) AND " +
            "(:startTime IS NULL OR ae.eventTime >= :startTime) AND " +
            "(:endTime IS NULL OR ae.eventTime <= :endTime)")
    Page<AuditEntry> findByComplexCriteria(@Param("userId") String userId,
                                           @Param("type") AuditEventType type,
                                           @Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime,
                                           Pageable pageable);
}