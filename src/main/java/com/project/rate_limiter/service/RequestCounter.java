package com.project.rate_limiter.service;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@AllArgsConstructor
public class RequestCounter {
    private AtomicInteger count;
    private long windowStart;

    public RequestCounter(){
        this.windowStart = System.currentTimeMillis();
        this.count = new AtomicInteger(0);
    }
}
