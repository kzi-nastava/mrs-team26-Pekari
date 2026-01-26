package com.pekara.repository;

import com.pekara.model.DriverWorkLog;
import com.pekara.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DriverWorkLogRepository extends JpaRepository<DriverWorkLog, Long> {

    /**
     * Find all work logs for a driver since a given time.
     * Used for display/history purposes.
     */
    @Query("SELECT w FROM DriverWorkLog w WHERE w.driver.id = :driverId AND w.startedAt >= :since")
    List<DriverWorkLog> findSince(@Param("driverId") Long driverId, @Param("since") LocalDateTime since);

    /**
     * Find only COMPLETED work logs for a driver since a given time.
     * Used for calculating the 8-hour work limit - only actual completed rides count.
     */
    @Query("SELECT w FROM DriverWorkLog w WHERE w.driver.id = :driverId AND w.completed = true AND w.startedAt >= :since")
    List<DriverWorkLog> findCompletedSince(@Param("driverId") Long driverId, @Param("since") LocalDateTime since);

    /**
     * Find work log by ride.
     */
    Optional<DriverWorkLog> findByRide(Ride ride);

    /**
     * Find work log by ride ID.
     */
    @Query("SELECT w FROM DriverWorkLog w WHERE w.ride.id = :rideId")
    Optional<DriverWorkLog> findByRideId(@Param("rideId") Long rideId);
}
