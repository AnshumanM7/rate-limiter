package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("sliding-window")
public class SlidingWindowStrategy implements RateLimiterStrategy{
    private final RateLimiterProperties properties;
    // to track request timestamps for each clientKey
    private final Map<String, Deque<Long>> clientRequests = new ConcurrentHashMap<>();
    public SlidingWindowStrategy(RateLimiterProperties properties){
        this.properties = properties;
    }
    @Override
    public boolean allowRequest(String clientKey){
        long now = System.currentTimeMillis();
        clientRequests.putIfAbsent(clientKey,new LinkedList<>());
        Deque<Long> timestamps = clientRequests.get(clientKey); // DS to store request timestamps
        synchronized (timestamps){
            long window = properties.getWindowSeconds() * 1000L;
            while(!timestamps.isEmpty() && now - timestamps.peekFirst() >= window){
                timestamps.pollFirst();
            }
            if(timestamps.size() < properties.getMaxRequests()){
                timestamps.addLast(now);
                return true;
            }
            else
                return false;
        }
    }
}
