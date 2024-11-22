package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.RuleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RuleHistoryRepository extends JpaRepository<RuleHistory, Long> {

    List<RuleHistory> findByRuleIdOrderByAuditInfo_CreatedAtDesc(Long ruleId);

    Page<RuleHistory> findByRuleIdOrderByTimestampDesc(Long ruleId, Pageable pageable);

    Optional<RuleHistory> findByRuleIdAndVersion(String ruleId, Integer version);

    int archiveRecordsOlderThan(LocalDateTime cutoffDate);

    Page<RuleHistory> findByProductId(String productId, Pageable pageable);

    List<RuleHistory> findByRuleIdAndChangeTypeInOrderByTimestampDesc(String ruleId, List<String> list);

    Optional<Long> findMaxVersionByRuleId(Long ruleId);

    Page<RuleHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<RuleHistory> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<RuleHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    List<RuleHistory> findByBatchIdOrderByTimestampDesc(String batchId);

    Page<RuleHistory> findByRuleIdStringOrderByTimestampDesc(String ruleId, Pageable pageable);

    Optional<RuleHistory> findByRuleIdStringAndVersion(String ruleId, Integer version);
}