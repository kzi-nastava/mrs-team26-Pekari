package com.pekara.repository;

import com.pekara.model.DriverWorkLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DriverWorkLogRepository extends JpaRepository<DriverWorkLog, Long> {

    @Query("SELECT w FROM DriverWorkLog w WHERE w.driver.id = :driverId AND w.startedAt >= :since")
    List<DriverWorkLog> findSince(@Param("driverId") Long driverId, @Param("since") LocalDateTime since);
}
