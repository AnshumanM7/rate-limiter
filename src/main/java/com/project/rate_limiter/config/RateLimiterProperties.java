package com.project.rate_limiter.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "rate-limiter")
@Setter
@Getter
public class RateLimiterProperties {
    @Getter
    @Setter
    private int maxRequests;
    @Getter
    @Setter
    private int windowSeconds;
    private String strategy;
}
