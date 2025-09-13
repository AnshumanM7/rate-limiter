package com.project.rate_limiter.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController //Indicates HTTP requests being handled

@RequestMapping("/api")//sets base-path for all endpoints in this class-> every endpoint starts with api

public class RateLimiterController {
    @GetMapping("/health")//specifies the following method is an HTTP GET method with "health" as its endpoint

    public ResponseEntity<String> healthCheck(){

        return ResponseEntity.ok("Rate Limiter API is up and running");
    }
}

// TODO: Add an endpoint like `/api/rate-limiter/stats`
