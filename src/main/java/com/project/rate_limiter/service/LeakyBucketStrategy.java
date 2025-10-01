package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component("leaky-bucket")
public class LeakyBucketStrategy implements RateLimiterStrategy {
    private final int capacity, leakRate;
    private int request = 0;
    private long lastLeakTimeStamp;

    public LeakyBucketStrategy(RateLimiterProperties properties){
        this.capacity = properties.getLeakyCapacity();
        this.leakRate = properties.getLeakyRate();
        this.lastLeakTimeStamp = System.currentTimeMillis();
    }
    private synchronized void leak(){
        long now = System.currentTimeMillis();
        long elapsed = now - lastLeakTimeStamp;
        int leaked = (int)((elapsed/1000.0)*leakRate);

        if(leaked > 0){
            request=Math.max(0,request - leaked);
            lastLeakTimeStamp = now;
        }
    }

    @Override
    public synchronized boolean allowRequest(String clientKey){
        leak();
        if(request < capacity){
            request++;
            return true;
        }
        return false;
    }
}
