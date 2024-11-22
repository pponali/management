package com.scaler.price.rule.repository;

import com.scaler.price.rule.domain.RuleVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RuleVersionRepository extends JpaRepository<RuleVersion, Long> {

    List<RuleVersion> findByRuleIdOrderByVersionNumberDesc(Long ruleId);

    @Query("SELECT MAX(rv.versionNumber) FROM RuleVersion rv WHERE rv.ruleId = :ruleId")
    Integer findLatestVersionNumber(@Param("ruleId") Long ruleId);
}
