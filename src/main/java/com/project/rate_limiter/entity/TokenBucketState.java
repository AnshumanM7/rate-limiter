package com.project.rate_limiter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "token_bucket_state")
@Data
public class TokenBucketState {
    @Id
    private String clientKey;
    private int tokens;
    private long lastRefillTimestamp;
}
