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

    private String strategy;
    @Getter
    @Setter
    private int maxRequests;
    @Getter
    @Setter
    private int windowSeconds;

    @Getter
    @Setter
    private int leakyCapacity;
    @Getter
    @Setter
    private int leakyRate;

    @Getter
    @Setter
    private boolean isEnabled;

}
