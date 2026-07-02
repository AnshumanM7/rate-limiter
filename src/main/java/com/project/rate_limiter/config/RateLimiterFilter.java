package com.project.rate_limiter.config;

import com.project.rate_limiter.service.RateLimiterStrategy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
public class RateLimiterFilter extends OncePerRequestFilter {
    private final RateLimiterProperties properties;
    private final Map<String, RateLimiterStrategy> strategies;

    public RateLimiterFilter(RateLimiterProperties properties, Map<String,RateLimiterStrategy> strategies) {
        this.properties = properties;
        this.strategies = strategies;
        System.out.println("STRATEGY PROPERTY = " + properties.getStrategy());
        System.out.println("AVAILABLE STRATEGIES:");
        strategies.forEach((k, v) -> System.out.println(" - " + k + " -> " + v.getClass().getSimpleName()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if(!properties.isEnabled()){
            filterChain.doFilter(request,response);
            return;
        }
        String clientKey = request.getHeader("X-API-KEY");
        if (clientKey == null || clientKey.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Missing API Key");
            return;
        }
        String strategyKey = properties.getStrategy().toLowerCase().replace(" ", "-").replace("_", "-");;
        RateLimiterStrategy strategy = strategies.get(strategyKey);
        if(strategy == null){
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Rate Limiting strategy not configured properly");
        }
        else if(strategy.allowRequest(clientKey)){
            filterChain.doFilter(request,response);
        }
        else {
            response.setStatus(429);
            response.getWriter().write("Rate Limit exceeded. Please try again later");
        }
    }
}
