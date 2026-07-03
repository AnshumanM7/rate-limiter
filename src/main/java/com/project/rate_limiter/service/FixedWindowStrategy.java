package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.entity.FixedWindowState;
import com.project.rate_limiter.repository.FixedWindowStateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("fixed-window")
public class FixedWindowStrategy implements RateLimiterStrategy{
    private final RateLimiterProperties properties;
    private final FixedWindowStateRepository repository;

    public FixedWindowStrategy(RateLimiterProperties properties, FixedWindowStateRepository repository){
        this.properties = properties;
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean allowRequest(String clientKey){
        long currentTime = System.currentTimeMillis();

        FixedWindowState state = repository.findByClientKeyWithLock(clientKey)
                .orElseGet(() -> {
                    FixedWindowState newState = new FixedWindowState();
                    newState.setClientKey(clientKey);
                    newState.setWindowStart(currentTime);
                    newState.setRequestCount(0);
                    return repository.saveAndFlush(newState);
                });

        if(currentTime - state.getWindowStart() >= properties.getWindowSeconds()*1000L){
            state.setWindowStart(currentTime);
            state.setRequestCount(0);
        }

        int newCount = state.getRequestCount() + 1;
        state.setRequestCount(newCount);
        repository.save(state);

        return newCount <= properties.getMaxRequests();
    }
}
