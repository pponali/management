package com.scaler.price.core.management.repository;

import com.scaler.price.core.management.dto.PriceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface EventRepository extends JpaRepository<PriceEvent, Long> {

    List<PriceEvent> findByPriceIdOrderByVersionAsc(String priceId);
}
