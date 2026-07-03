package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.entity.LeakyBucketState;
import com.project.rate_limiter.repository.LeakyBucketStateRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Component("leaky-bucket")
public class LeakyBucketStrategy implements RateLimiterStrategy {
    private final int capacity, leakRate;
    private final LeakyBucketStateRepository repository;

    public LeakyBucketStrategy(RateLimiterProperties properties, LeakyBucketStateRepository repository){
        this.capacity = properties.getLeakyCapacity();
        this.leakRate = properties.getLeakyRate();
        this.repository = repository;
        if (this.leakRate >= this.capacity) {
            throw new IllegalArgumentException("Leak rate (" + this.leakRate + ") must be less than capacity (" + this.capacity + ").");
        }
        log.info("LeakyBucketStrategy Initialized - Capacity: {}, Leak Rate: {}", this.capacity, this.leakRate);
    }

    private void leak(LeakyBucketState state){
        long now = System.currentTimeMillis();
        long elapsed = now - state.getLastLeakTimestamp();
        int leaked = (int)((elapsed/1000.0)*leakRate);

        if(leaked > 0){
            log.info("Leaky Bucket Leaking: {} requests (elapsed: {}ms)", leaked, elapsed);
            state.setCurrentRequests(Math.max(0, state.getCurrentRequests() - leaked));
            state.setLastLeakTimestamp(now);
        }
    }

    @Override
    @Transactional
    public boolean allowRequest(String clientKey){
        long now = System.currentTimeMillis();

        LeakyBucketState state = repository.findByClientKeyWithLock(clientKey)
                .orElseGet(() -> {
                    LeakyBucketState newState = new LeakyBucketState();
                    newState.setClientKey(clientKey);
                    newState.setLastLeakTimestamp(now);
                    newState.setCurrentRequests(0);
                    return repository.saveAndFlush(newState);
                });

        leak(state);

        boolean allowed = state.getCurrentRequests() < capacity;
        log.info("Leaky Bucket Request - Key: {} | Bucket: {}/{} | Allowed: {}", clientKey, state.getCurrentRequests(), capacity, allowed);

        if(allowed){
            state.setCurrentRequests(state.getCurrentRequests() + 1);
            repository.save(state);
            return true;
        }
        return false;
    }
}
