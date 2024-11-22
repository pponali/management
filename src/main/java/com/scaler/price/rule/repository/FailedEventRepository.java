package com.scaler.price.rule.repository;


import com.scaler.price.rule.domain.FailedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FailedEventRepository extends JpaRepository<FailedEventEntity, String> {
}
