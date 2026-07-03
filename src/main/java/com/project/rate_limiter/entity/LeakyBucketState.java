package com.project.rate_limiter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "leaky_bucket_state")
@Data
public class LeakyBucketState {
    @Id
    private String clientKey;
    private int currentRequests;
    private long lastLeakTimestamp;
}
