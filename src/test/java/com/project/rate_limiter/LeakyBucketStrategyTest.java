package com.project.rate_limiter;

import com.project.rate_limiter.config.RateLimiterProperties;
import com.project.rate_limiter.repository.LeakyBucketStateRepository;
import com.project.rate_limiter.service.LeakyBucketStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class LeakyBucketStrategyTest {

    @Autowired
    private LeakyBucketStateRepository repository;

    private LeakyBucketStrategy strategy;
    private RateLimiterProperties properties;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
        properties = new RateLimiterProperties();
    }

    @Test
    void testBucketFillsAndBlocks() {
        properties.setLeakyCapacity(3);
        properties.setLeakyRate(1);
        strategy = new LeakyBucketStrategy(properties, repository);
        String clientKey = "client-leaky-1";

        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        // Bucket is full, 4th request must fail
        assertFalse(strategy.allowRequest(clientKey));
    }

    @Test
    void testBucketLeaking() throws InterruptedException {
        properties.setLeakyCapacity(2);
        properties.setLeakyRate(1); // Leaks 1 request per second
        strategy = new LeakyBucketStrategy(properties, repository);
        String clientKey = "client-leaky-2";

        assertTrue(strategy.allowRequest(clientKey));
        assertTrue(strategy.allowRequest(clientKey));
        assertFalse(strategy.allowRequest(clientKey)); // full

        // Wait 1.1 seconds for 1 leak
        Thread.sleep(1100);

        assertTrue(strategy.allowRequest(clientKey)); // allowed
        assertFalse(strategy.allowRequest(clientKey)); // full again
    }

    @Test
    void testInvalidConfigurationThrows() {
        properties.setLeakyCapacity(3);
        properties.setLeakyRate(3); // leakRate >= capacity

        // Should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            new LeakyBucketStrategy(properties, repository);
        });
    }
}
