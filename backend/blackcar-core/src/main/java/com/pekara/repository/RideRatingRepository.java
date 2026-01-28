package com.pekara.repository;

import com.pekara.model.RideRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RideRatingRepository extends JpaRepository<RideRating, Long> {

    @Query("SELECT rr FROM RideRating rr WHERE rr.ride.id = :rideId")
    Optional<RideRating> findByRideId(@Param("rideId") Long rideId);

    @Query("SELECT rr FROM RideRating rr WHERE rr.ride.id = :rideId AND rr.passenger.id = :passengerId")
    Optional<RideRating> findByRideIdAndPassengerId(@Param("rideId") Long rideId, @Param("passengerId") Long passengerId);

    @Query("SELECT CASE WHEN COUNT(rr) > 0 THEN true ELSE false END FROM RideRating rr WHERE rr.ride.id = :rideId AND rr.passenger.id = :passengerId")
    boolean existsByRideIdAndPassengerId(@Param("rideId") Long rideId, @Param("passengerId") Long passengerId);
}
