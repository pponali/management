package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.RuleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RuleHistoryRepository extends JpaRepository<RuleHistory, Long> {

    Page<RuleHistory> findByRuleIdOrderByTimestampDesc(Long ruleId, Pageable pageable);

    Optional<RuleHistory> findByRuleIdAndVersion(Long ruleId, Integer version);

    @Modifying
    @Query("DELETE FROM RuleHistory rh WHERE rh.timestamp < :cutoffDate")
    int deleteByTimestampBefore(@Param("cutoffDate") LocalDateTime cutoffDate);

    Page<RuleHistory> findByProductId(Long productId, Pageable pageable);

    List<RuleHistory> findByRuleIdAndChangeTypeInOrderByTimestampDesc(Long ruleId, List<String> list);

    Optional<Long> findMaxVersionByRuleId(Long ruleId);

    Page<RuleHistory> findByUserIdOrderByTimestampDesc(String userId, Pageable pageable);

    Page<RuleHistory> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    Page<RuleHistory> findAllByOrderByTimestampDesc(Pageable pageable);

    List<RuleHistory> findByBatchIdOrderByTimestampDesc(String batchId);

}