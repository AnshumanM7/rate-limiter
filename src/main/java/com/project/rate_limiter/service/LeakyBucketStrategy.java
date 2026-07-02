package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component("leaky-bucket")
public class LeakyBucketStrategy implements RateLimiterStrategy {
    private final int capacity, leakRate;
    private int request = 0;
    private long lastLeakTimeStamp;

    public LeakyBucketStrategy(RateLimiterProperties properties){
        this.capacity = properties.getLeakyCapacity();
        this.leakRate = properties.getLeakyRate();
        if (this.leakRate >= this.capacity) {
            throw new IllegalArgumentException("Leak rate (" + this.leakRate + ") must be less than capacity (" + this.capacity + ").");
        }
        this.lastLeakTimeStamp = System.currentTimeMillis();
        log.info("LeakyBucketStrategy Initialized - Capacity: {}, Leak Rate: {}", this.capacity, this.leakRate);
    }
    private synchronized void leak(){
        long now = System.currentTimeMillis();
        long elapsed = now - lastLeakTimeStamp;
        int leaked = (int)((elapsed/1000.0)*leakRate);

        if(leaked > 0){
            log.info("Leaky Bucket Leaking: {} requests (elapsed: {}ms)", leaked, elapsed);
            request=Math.max(0,request - leaked);
            lastLeakTimeStamp = now;
        }
    }

    @Override
    public synchronized boolean allowRequest(String clientKey){
        leak();
        boolean allowed = request < capacity;
        log.info("Leaky Bucket Request - Key: {} | Bucket: {}/{} | Allowed: {}", clientKey, request, capacity, allowed);
        if(allowed){
            request++;
            return true;
        }
        return false;
    }
}
