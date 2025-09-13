package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Component("fixed-window")
public class FixedWindowStrategy implements RateLimiterStrategy{
    private final RateLimiterProperties properties;
    private final Map<String,RequestCounter> requestCounts = new ConcurrentHashMap<>();

    public FixedWindowStrategy(RateLimiterProperties properties){
        this.properties = properties;
    }
    @Override
    public boolean allowRequest(String clientKey){
        RequestCounter counter = requestCounts.computeIfAbsent(clientKey,k->new RequestCounter());

        synchronized (counter){
            long currentTime = System.currentTimeMillis();

            if(currentTime - counter.getWindowStart() >= properties.getWindowSeconds()*1000L){
                counter.setWindowStart(currentTime);
                counter.getCount().set(0);
            }
            return counter.getCount().incrementAndGet() <= properties.getMaxRequests();
        }
    }
}
