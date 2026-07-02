package com.project.rate_limiter;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.service.FixedWindowStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowStrategyTest {

    @Test
    void testAllowRequestWithinLimit() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(5);
        properties.setWindowSeconds(10);

        FixedWindowStrategy strategy = new FixedWindowStrategy(properties);
        String clientKey = "client-fixed-1";

        // All 5 requests should be allowed
        for (int i = 0; i < 5; i++) {
            assertTrue(strategy.allowRequest(clientKey));
        }
    }

    @Test
    void testBlockRequestOverLimit() {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(2);
        properties.setWindowSeconds(10);

        FixedWindowStrategy strategy = new FixedWindowStrategy(properties);
        String clientKey = "client-fixed-2";

        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        // 3rd request must be blocked
        assertFalse(strategy.allowRequest(clientKey));
    }

    @Test
    void testWindowReset() throws InterruptedException {
        RateLimiterProperties properties = new RateLimiterProperties();
        properties.setMaxRequests(1);
        properties.setWindowSeconds(1); // 1 second window

        FixedWindowStrategy strategy = new FixedWindowStrategy(properties);
        String clientKey = "client-fixed-3";

        assertTrue(strategy.allowRequest(clientKey));
        assertFalse(strategy.allowRequest(clientKey)); // blocked

        // Wait for window to elapse
        Thread.sleep(1100);

        // Should be allowed in the new window
        assertTrue(strategy.allowRequest(clientKey));
    }
}
