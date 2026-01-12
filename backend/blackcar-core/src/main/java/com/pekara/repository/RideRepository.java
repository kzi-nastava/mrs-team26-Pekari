package com.pekara.repository;

import com.pekara.constant.RideStatus;
import com.pekara.model.Ride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    @Query("SELECT r FROM Ride r WHERE r.status = :status AND r.scheduledAt IS NOT NULL AND r.scheduledAt > :now AND r.scheduledAt <= :upper")
    List<Ride> findScheduledRidesStartingBefore(@Param("status") RideStatus status,
                                               @Param("now") LocalDateTime now,
                                               @Param("upper") LocalDateTime upper);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.status = :status AND r.startedAt >= :since")
    List<Ride> findDriverRidesSince(@Param("driverId") Long driverId,
                                   @Param("status") RideStatus status,
                                   @Param("since") LocalDateTime since);

    @Query("SELECT r FROM Ride r WHERE r.driver.id = :driverId AND r.status = :inProgress")
    List<Ride> findDriverInProgressRides(@Param("driverId") Long driverId,
                                         @Param("inProgress") RideStatus inProgress);
}
