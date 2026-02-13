package com.pekara.repository;

import com.pekara.model.InconsistencyReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InconsistencyReportRepository extends JpaRepository<InconsistencyReport, Long> {

    @Query("SELECT ir FROM InconsistencyReport ir WHERE ir.ride.id = :rideId ORDER BY ir.createdAt DESC")
    List<InconsistencyReport> findAllByRideId(@Param("rideId") Long rideId);
}
