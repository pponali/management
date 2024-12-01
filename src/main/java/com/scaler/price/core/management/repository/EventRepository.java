package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.domain.PriceEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EventRepository extends JpaRepository<PriceEventEntity, Long> {

    List<PriceEventEntity> findByPriceIdOrderByVersionAsc(Long priceId);
}
