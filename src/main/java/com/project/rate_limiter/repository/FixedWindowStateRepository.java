package com.project.rate_limiter.repository;

import com.project.rate_limiter.entity.FixedWindowState;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FixedWindowStateRepository extends JpaRepository<FixedWindowState, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM FixedWindowState s WHERE s.clientKey = :clientKey")
    Optional<FixedWindowState> findByClientKeyWithLock(@Param("clientKey") String clientKey);
}
