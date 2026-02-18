package com.pekara.repository;

import com.pekara.config.TestConfig;
import com.pekara.model.Driver;
import com.pekara.model.DriverWorkLog;
import com.pekara.model.Ride;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.testng.AbstractTransactionalTestNGSpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for DriverWorkLogRepository - ride ordering (8h work limit: findCompletedSince, findSince, findByRideId).
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class DriverWorkLogRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private DriverWorkLogRepository driverWorkLogRepository;

    private Driver driver;
    private Ride ride;

    @BeforeMethod
    public void setUp() {
        entityManager.clear();

        User creator = User.builder()
                .email("creator@test.com")
                .username("creator")
                .password("password")
                .firstName("Creator")
                .lastName("User")
                .phoneNumber("+381641111111")
                .address("Address")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .totalRides(0)
                .build();

        driver = Driver.builder()
                .email("driver@test.com")
                .username("driver")
                .password("password")
                .firstName("Bob")
                .lastName("Driver")
                .phoneNumber("+381642222222")
                .address("Driver Address")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SEDAN")
                .licensePlate("NS-123-AB")
                .build();

        entityManager.persist(creator);
        entityManager.persist(driver);
        entityManager.flush();

        ride = Ride.builder()
                .creator(creator)
                .driver(driver)
                .status(com.pekara.constant.RideStatus.COMPLETED)
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .estimatedPrice(java.math.BigDecimal.valueOf(500))
                .distanceKm(10.0)
                .estimatedDurationMinutes(20)
                .routeCoordinates("[]")
                .build();
        ride.getPassengers().add(creator);
        entityManager.persist(ride);
        entityManager.flush();
    }

    @Test(description = "Should return completed work logs since given time")
    public void findCompletedSince_CompletedAfterSince_ReturnsThem() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        LocalDateTime started = since.plusHours(1);
        LocalDateTime ended = started.plusHours(2);

        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .ride(ride)
                .startedAt(started)
                .endedAt(ended)
                .completed(true)
                .createdAt(started)
                .build();
        entityManager.persist(log);
        entityManager.flush();

        List<DriverWorkLog> result = driverWorkLogRepository.findCompletedSince(driver.getId(), since);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCompleted()).isTrue();
        assertThat(result.get(0).getStartedAt()).isAfter(since);
    }

    @Test(description = "Should not return work logs started before since")
    public void findCompletedSince_StartedBeforeSince_ReturnsEmpty() {
        LocalDateTime since = LocalDateTime.now().minusHours(12);
        LocalDateTime started = since.minusHours(2);
        LocalDateTime ended = started.plusHours(1);

        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .ride(ride)
                .startedAt(started)
                .endedAt(ended)
                .completed(true)
                .createdAt(started)
                .build();
        entityManager.persist(log);
        entityManager.flush();

        List<DriverWorkLog> result = driverWorkLogRepository.findCompletedSince(driver.getId(), since);

        assertThat(result).isEmpty();
    }

    @Test(description = "Should not return incomplete work logs")
    public void findCompletedSince_NotCompleted_ReturnsEmpty() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        LocalDateTime started = since.plusHours(1);

        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .ride(ride)
                .startedAt(started)
                .endedAt(null)
                .completed(false)
                .createdAt(started)
                .build();
        entityManager.persist(log);
        entityManager.flush();

        List<DriverWorkLog> result = driverWorkLogRepository.findCompletedSince(driver.getId(), since);

        assertThat(result).isEmpty();
    }

    @Test(description = "Should return all work logs for driver since given time")
    public void findSince_ReturnsAllLogsAfterSince() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        LocalDateTime started1 = since.plusHours(1);
        LocalDateTime started2 = since.plusHours(5);

        DriverWorkLog log1 = DriverWorkLog.builder()
                .driver(driver)
                .ride(ride)
                .startedAt(started1)
                .endedAt(started1.plusHours(1))
                .completed(true)
                .createdAt(started1)
                .build();
        DriverWorkLog log2 = DriverWorkLog.builder()
                .driver(driver)
                .ride(null)
                .startedAt(started2)
                .endedAt(null)
                .completed(false)
                .createdAt(started2)
                .build();
        entityManager.persist(log1);
        entityManager.persist(log2);
        entityManager.flush();

        List<DriverWorkLog> result = driverWorkLogRepository.findSince(driver.getId(), since);

        assertThat(result).hasSize(2);
    }

    @Test(description = "Should return work log by ride id")
    public void findByRideId_ExistingRide_ReturnsWorkLog() {
        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .ride(ride)
                .startedAt(LocalDateTime.now().minusHours(1))
                .endedAt(LocalDateTime.now())
                .completed(true)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();
        entityManager.persist(log);
        entityManager.flush();

        Optional<DriverWorkLog> result = driverWorkLogRepository.findByRideId(ride.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getRide().getId()).isEqualTo(ride.getId());
    }

    @Test(description = "Should return empty when ride id has no work log")
    public void findByRideId_NonExistent_ReturnsEmpty() {
        Optional<DriverWorkLog> result = driverWorkLogRepository.findByRideId(99999L);
        assertThat(result).isEmpty();
    }
}
