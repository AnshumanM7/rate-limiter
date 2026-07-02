package com.project.rate_limiter;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.service.TokenBucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    @Test
    void testTokenConsumption() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1); // 1s / 2 = 500ms refill interval

        TokenBucket strategy = new TokenBucket(properties);
        String clientKey = "client-token-1";

        assertTrue(strategy.allowRequest(clientKey)); // uses token 1
        assertTrue(strategy.allowRequest(clientKey)); // uses token 2
        assertFalse(strategy.allowRequest(clientKey)); // empty, blocked
    }

    @Test
    void testTokenRefill() throws InterruptedException {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1); // refill rate: 1 token every 500ms

        TokenBucket strategy = new TokenBucket(properties);
        String clientKey = "client-token-2";

        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        assertFalse(strategy.allowRequest(clientKey));

        // Wait 600ms to allow 1 token to refill
        Thread.sleep(600);

        assertTrue(strategy.allowRequest(clientKey)); // allowed
        assertFalse(strategy.allowRequest(clientKey)); // empty again
    }

    @Test
    void testTokenCapacityLimit() throws InterruptedException {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1); // 500ms refill

        TokenBucket strategy = new TokenBucket(properties);
        String clientKey = "client-token-3";

        // Wait long enough that many tokens could have refilled
        Thread.sleep(2000);

        // Verify tokens are capped at max capacity (2), not more
        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        assertFalse(strategy.allowRequest(clientKey)); // 3rd must fail
    }
}
