package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.entity.SlidingWindowRequest;
import com.project.rate_limiter.repository.SlidingWindowRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("sliding-window")
public class SlidingWindowStrategy implements RateLimiterStrategy{
    private final RateLimiterProperties properties;
    private final SlidingWindowRequestRepository repository;

    public SlidingWindowStrategy(RateLimiterProperties properties, SlidingWindowRequestRepository repository){
        this.properties = properties;
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean allowRequest(String clientKey){
        long now = System.currentTimeMillis();
        long windowStartThreshold = now - (properties.getWindowSeconds() * 1000L);

        // 1. Clean up old requests from the database that are outside the sliding window
        repository.deleteByClientKeyAndRequestTimestampLessThan(clientKey, windowStartThreshold);

        // 2. Count active requests within the window
        long activeRequestsCount = repository.countByClientKeyAndRequestTimestampGreaterThan(clientKey, windowStartThreshold);

        // 3. Check against limit and log the current request if allowed
        if(activeRequestsCount < properties.getMaxRequests()){
            SlidingWindowRequest log = new SlidingWindowRequest();
            log.setClientKey(clientKey);
            log.setRequestTimestamp(now);
            repository.save(log);
            return true;
        }

        return false;
    }
}
