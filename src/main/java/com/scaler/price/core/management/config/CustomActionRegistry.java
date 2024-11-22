package com.scaler.price.core.management.config;

import com.scaler.price.rule.actions.handler.CustomActionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CustomActionRegistry {
    private final Map<String, CustomActionHandler> handlers = new ConcurrentHashMap<>();

    public void registerHandler(String name, CustomActionHandler handler) {
        handlers.put(name, handler);
        log.info("Registered custom action handler: {}", name);
    }

    public CustomActionHandler getHandler(String name) {
        return handlers.get(name);
    }
}
