package com.project.rate_limiter;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.repository.SlidingWindowRequestRepository;
import com.project.rate_limiter.service.SlidingWindowStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class SlidingWindowStrategyTest {

    @Autowired
    private SlidingWindowRequestRepository repository;

    private SlidingWindowStrategy strategy;
    private RateLimiterProperties properties;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        properties = new RateLimiterProperties();
    }

    @Test
    void testBurstRequestWithinLimit() {
        properties.setMaxRequests(3);
        properties.setWindowSeconds(5);
        strategy = new SlidingWindowStrategy(properties, repository);

        String clientKey = "client-sliding-1";

        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
    }

    @Test
    void testRollingWindowEviction() throws InterruptedException {
        properties.setMaxRequests(2);
        properties.setWindowSeconds(1); // 1 second rolling window
        strategy = new SlidingWindowStrategy(properties, repository);

        String clientKey = "client-sliding-2";

        assertTrue(strategy.allowRequest(clientKey)); // request at 0ms
        Thread.sleep(400);
        assertTrue(strategy.allowRequest(clientKey)); // request at 400ms

        // 3rd request at 400ms is blocked
        assertFalse(strategy.allowRequest(clientKey));

        // Wait so we are past 1000ms from the 1st request (but not from the 2nd request)
        Thread.sleep(700);

        // 1st request has slid out, so 1 slot is open
        assertTrue(strategy.allowRequest(clientKey));
        
        // 2nd request is still within the 1-second window, so this 4th request gets blocked
        assertFalse(strategy.allowRequest(clientKey));
    }

    @Test
    void testDifferentClientsIndependentLimits() {
        properties.setMaxRequests(1);
        properties.setWindowSeconds(10);
        strategy = new SlidingWindowStrategy(properties, repository);

        // Client A consumes their limit
        assertTrue(strategy.allowRequest("client-A"));
        assertFalse(strategy.allowRequest("client-A"));

        // Client B is unaffected and can still make a request
        assertTrue(strategy.allowRequest("client-B"));
        assertFalse(strategy.allowRequest("client-B"));
    }
}
