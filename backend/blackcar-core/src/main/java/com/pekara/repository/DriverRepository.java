package com.pekara.repository;

import com.pekara.model.Driver;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByLicenseNumber(String licenseNumber);

    Optional<Driver> findByEmail(String email);

    boolean existsByLicenseNumber(String licenseNumber);

    @Query("SELECT d FROM Driver d WHERE d.isActive = true")
    List<Driver> findAllActiveDrivers();
}
