package com.project.rate_limiter.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "fixed_window_state")
@Data
public class FixedWindowState {
    @Id
    private String clientKey;
    private int requestCount;
    private long windowStart;
}
