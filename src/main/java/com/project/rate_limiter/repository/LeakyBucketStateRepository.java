package com.project.rate_limiter.repository;

import com.project.rate_limiter.entity.LeakyBucketState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeakyBucketStateRepository extends JpaRepository<LeakyBucketState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM LeakyBucketState s WHERE s.clientKey = :clientKey")
    Optional<LeakyBucketState> findByClientKeyWithLock(@Param("clientKey") String clientKey);
}
