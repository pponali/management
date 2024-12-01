package com.scaler.price.core.management.service;


import com.scaler.price.core.management.domain.PriceEventEntity;
import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.core.management.repository.EventRepository;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@NoArgsConstructor
@AllArgsConstructor
public class PriceEventStore {
    @Autowired
    private EventRepository eventRepository;

    public void storeEvent(PriceEventEntity event) {
        event.setVersion(getNextVersion());
        eventRepository.save(event);
    }

    private Long getNextVersion() {
        return 1L;
    }

    public List<PriceEventEntity> replayEvents(Long priceId) {
        return eventRepository.findByPriceIdOrderByVersionAsc(priceId);
    }
}
