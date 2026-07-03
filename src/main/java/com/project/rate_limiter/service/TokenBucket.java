package com.project.rate_limiter.service;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.entity.TokenBucketState;
import com.project.rate_limiter.repository.TokenBucketStateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("token-bucket")
public class TokenBucket implements RateLimiterStrategy{
    private final TokenBucketStateRepository repository;
    private final int capacity;
    private final int refillTokens;
    private final long refillInterval;

    public TokenBucket(RateLimiterProperties properties, TokenBucketStateRepository repository){
        this.repository = repository;
        this.capacity = properties.getMaxRequests();
        this.refillInterval = (properties.getWindowSeconds()*1000L)/properties.getMaxRequests();
        this.refillTokens = 1;
    }

    @Override
    @Transactional
    public boolean allowRequest(String clientId){
        long now = System.nanoTime();

        TokenBucketState state = repository.findByClientKeyWithLock(clientId)
                .orElseGet(() -> {
                    TokenBucketState newState = new TokenBucketState();
                    newState.setClientKey(clientId);
                    newState.setTokens(capacity);
                    newState.setLastRefillTimestamp(now);
                    return repository.saveAndFlush(newState);
                });

        refill(state, now);

        if(state.getTokens() > 0){
            state.setTokens(state.getTokens() - 1);
            repository.save(state);
            return true;
        }

        return false;
    }

    private void refill(TokenBucketState state, long now){
        long elapsedTime = now - state.getLastRefillTimestamp();
        long tokensToAdd = (elapsedTime/(refillInterval * 1_000_000L))*refillTokens;

        if(tokensToAdd > 0){
            state.setTokens(Math.min(capacity, state.getTokens() + (int)tokensToAdd));
            state.setLastRefillTimestamp(now);
        }
    }
}
