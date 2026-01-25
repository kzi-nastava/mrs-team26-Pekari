package com.pekara.repository;

import com.pekara.model.DriverState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverStateRepository extends JpaRepository<DriverState, Long> {

    @Query("SELECT ds FROM DriverState ds WHERE ds.online = true")
    Page<DriverState> findOnlineDrivers(Pageable pageable);

    @Query("SELECT ds FROM DriverState ds WHERE ds.online = true")
    List<DriverState> findAllOnlineDrivers();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ds FROM DriverState ds WHERE ds.id = :driverId")
    Optional<DriverState> findByDriverIdForUpdate(@Param("driverId") Long driverId);
}
