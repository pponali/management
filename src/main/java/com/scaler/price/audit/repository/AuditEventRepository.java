package com.scaler.price.audit.repository;

import com.scaler.price.audit.domain.AuditEntry;
import com.scaler.price.audit.domain.AuditEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEntry, Long> {

    /**
     * Find audit events by user ID
     * @param userId The ID of the user whose audit events are to be retrieved
     * @return List of audit events associated with the specified user
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.userId = :userId ORDER BY ae.timestamp DESC")
    List<AuditEntry> findByUserId(@Param("userId") String userId);

    /**
     * Find audit events by event type
     * @param eventType The type of audit events to retrieve
     * @return List of audit events of the specified type
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.eventType = :eventType ORDER BY ae.timestamp DESC")
    List<AuditEntry> findByEventType(@Param("eventType") AuditEventType eventType);

    /**
     * Find audit events within a time range
     */
    List<AuditEntry> findByTimestampBetween(Instant startTime, Instant endTime);

    /**
     * Find audit events by user ID and event type
     */
    List<AuditEntry> findByUserIdAndEventType(String userId, AuditEventType eventType);

    /**
     * Find audit events by user ID within a time range
     */
    List<AuditEntry> findByUserIdAndTimestampBetween(String userId, Instant startTime, Instant endTime);

    /**
     * Find audit events by event type within a time range
     */
    List<AuditEntry> findByEventTypeAndTimestampBetween(AuditEventType eventType, Instant startTime, Instant endTime);

    /**
     * Find paginated audit events
     */
    Page<AuditEntry> findAll(Pageable pageable);

    /**
     * Find paginated audit events by user ID
     */
    Page<AuditEntry> findByUserId(String userId, Pageable pageable);

    /**
     * Find paginated audit events by event type
     */
    Page<AuditEntry> findByEventType(AuditEventType eventType, Pageable pageable);

    /**
     * Search audit events by event data content
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.eventData LIKE %:searchTerm%")
    List<AuditEntry> searchByEventData(@Param("searchTerm") String searchTerm);

    /**
     * Find latest audit event by rule ID
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.eventData LIKE %:ruleId% ORDER BY ae.timestamp DESC LIMIT 1")
    Optional<AuditEntry> findLatestByRuleId(@Param("ruleId") String ruleId);

    /**
     * Find all audit events for a specific rule
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE ae.eventData LIKE %:ruleId% ORDER BY ae.timestamp DESC")
    List<AuditEntry> findAllByRuleId(@Param("ruleId") String ruleId);

    /**
     * Count events by type within a time range
     */
    long countByEventTypeAndTimestampBetween(AuditEventType eventType, Instant startTime, Instant endTime);

    /**
     * Find events by multiple event types
     */
    List<AuditEntry> findByEventTypeIn(List<AuditEventType> eventTypes);

    /**
     * Find events by user ID and multiple event types
     */
    List<AuditEntry> findByUserIdAndEventTypeIn(String userId, List<AuditEventType> eventTypes);

    /**
     * Find events newer than a specific timestamp
     */
    List<AuditEntry> findByTimestampAfter(Instant timestamp);

    /**
     * Find events older than a specific timestamp
     */
    List<AuditEntry> findByTimestampBefore(Instant timestamp);

    /**
     * Custom query to find events with specific rule modifications
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.eventType = 'RULE_MODIFIED' " +
           "AND ae.eventData LIKE %:fieldName% AND ae.eventData LIKE %:fieldValue%")
    List<AuditEntry> findRuleModificationsByField(@Param("fieldName") String fieldName,
                                                 @Param("fieldValue") String fieldValue);

    /**
     * Find events by user ID and event type with pagination
     */
    Page<AuditEntry> findByUserIdAndEventType(String userId, AuditEventType eventType, Pageable pageable);

    /**
     * Custom query to get event statistics
     */
    @Query("SELECT ae.eventType, COUNT(ae) FROM AuditEntry ae " +
           "WHERE ae.timestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY ae.eventType")
    List<Object[]> getEventStatistics(@Param("startTime") Instant startTime, 
                                     @Param("endTime") Instant endTime);

    /**
     * Find distinct users who performed specific event type
     */
    @Query("SELECT DISTINCT ae.userId FROM AuditEntry ae WHERE ae.eventType = :eventType")
    List<String> findDistinctUsersByEventType(@Param("eventType") AuditEventType eventType);

    /**
     * Delete old audit events
     */
    void deleteByTimestampBefore(Instant timestamp);

    /**
     * Find events by complex criteria
     */
    @Query("SELECT ae FROM AuditEntry ae WHERE " +
           "(:userId IS NULL OR ae.userId = :userId) AND " +
           "(:eventType IS NULL OR ae.eventType = :eventType) AND " +
           "(:startTime IS NULL OR ae.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR ae.timestamp <= :endTime)")
    Page<AuditEntry> findByComplexCriteria(@Param("userId") String userId,
                                          @Param("eventType") AuditEventType eventType,
                                          @Param("startTime") Instant startTime,
                                          @Param("endTime") Instant endTime,
                                          Pageable pageable);
}