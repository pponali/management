package com.scaler.price.core.management.service;


import com.scaler.price.core.management.dto.PriceEvent;
import com.scaler.price.core.management.repository.EventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PriceEventStore {
    private final EventRepository eventRepository;

    public PriceEventStore(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    public void storeEvent(PriceEvent event) {
        event.setVersion(getNextVersion());
        eventRepository.save(event);
    }

    private int getNextVersion() {
        return 1;
    }

    public List<PriceEvent> replayEvents(String priceId) {
        return eventRepository.findByPriceIdOrderByVersionAsc(priceId);
    }
}
