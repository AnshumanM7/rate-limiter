package com.project.rate_limiter.service;

public interface RateLimiterStrategy {
    boolean allowRequest(String clientKey);
}
