package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component("token-bucket")
public class TokenBucket implements RateLimiterStrategy{
    private final Map<String,Bucket> clientBuckets = new ConcurrentHashMap<>();
    private final int capacity;
    private final int refillTokens;
    private final long refillInterval;

    public TokenBucket(RateLimiterProperties properties){
        this.capacity = properties.getMaxRequests();
        this.refillInterval = (properties.getWindowSeconds()*1000L)/properties.getMaxRequests();
        this.refillTokens = 1;
    }

    @Override
    public boolean allowRequest(String clientId){
        Bucket bucket = clientBuckets.computeIfAbsent(clientId, id->new Bucket(capacity,refillTokens,refillInterval));
        return bucket.tryConsume();
    }

    private static class Bucket{
        private int tokens;
        private final int capacity, refillTokens;
        private final long refillInterval;
        private long lastRefillTimestamp;

        public Bucket(int capacity,int refillTokens,long refillInterval){
            this.capacity = capacity;
            this.tokens = capacity;
            this.refillInterval = refillInterval;
            this.refillTokens = refillTokens;
            this.lastRefillTimestamp = System.nanoTime();
        }
        synchronized boolean tryConsume(){
            refill();
            if(tokens > 0){
                tokens--;
                return true;
            }
            return false;
        }
        private void refill(){
            long now = System.nanoTime();
            long elapsedTime = now - lastRefillTimestamp;
            long tokensToAdd = (elapsedTime/(refillInterval * 1_000_000))*refillTokens;

            if(tokensToAdd > 0){
                tokens = Math.min(capacity,tokens+(int)tokensToAdd);
                lastRefillTimestamp = now;
            }
        }
    }
}
