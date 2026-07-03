package com.project.rate_limiter.repository;

import com.project.rate_limiter.entity.SlidingWindowRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

public interface SlidingWindowRequestRepository extends JpaRepository<SlidingWindowRequest, Long> {

    long countByClientKeyAndRequestTimestampGreaterThan(String clientKey, long timestamp);

    @Transactional
    @Modifying
    void deleteByClientKeyAndRequestTimestampLessThan(String clientKey, long timestamp);
}
