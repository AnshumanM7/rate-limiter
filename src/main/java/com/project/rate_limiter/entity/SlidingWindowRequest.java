package com.project.rate_limiter.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "sliding_window_requests", indexes = {
    @Index(name = "idx_client_time", columnList = "clientKey, requestTimestamp")
})
@Data
public class SlidingWindowRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String clientKey;
    private long requestTimestamp;
}
