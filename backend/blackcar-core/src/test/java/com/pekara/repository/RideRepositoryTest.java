package com.pekara.repository;

import com.pekara.config.TestConfig;
import com.pekara.constant.RideStatus;
import com.pekara.model.Driver;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for RideRepository - Early Ride Stoppage functionality
 * Tests custom queries with STOP_REQUESTED status
 */
@SpringBootTest(classes = TestConfig.class)
@ActiveProfiles("test")
@Transactional
public class RideRepositoryTest extends AbstractTransactionalTestNGSpringContextTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private RideRepository rideRepository;

    private User passenger1;
    private User passenger2;
    private Driver driver1;
    private Driver driver2;

    @BeforeMethod
    public void setUp() {
        // Clear previous data
        entityManager.clear();

        passenger1 = User.builder()
                .email("passenger1@test.com")
                .username("passenger1")
                .password("password")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+381641111111")
                .address("Address 1")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .totalRides(0)
                .build();

        passenger2 = User.builder()
                .email("passenger2@test.com")
                .username("passenger2")
                .password("password")
                .firstName("Jane")
                .lastName("Smith")
                .phoneNumber("+381642222222")
                .address("Address 2")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .totalRides(0)
                .build();

        driver1 = Driver.builder()
                .email("driver1@test.com")
                .username("driver1")
                .password("password")
                .firstName("Bob")
                .lastName("Driver")
                .phoneNumber("+381643333333")
                .address("Address 3")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SEDAN")
                .licensePlate("NS-123-AB")
                .build();

        driver2 = Driver.builder()
                .email("driver2@test.com")
                .username("driver2")
                .password("password")
                .firstName("Alice")
                .lastName("Driver")
                .phoneNumber("+381644444444")
                .address("Address 4")
                .role(UserRole.DRIVER)
                .isActive(true)
                .totalRides(0)
                .vehicleType("SUV")
                .licensePlate("NS-456-CD")
                .build();

        entityManager.persist(passenger1);
        entityManager.persist(passenger2);
        entityManager.persist(driver1);
        entityManager.persist(driver2);
        entityManager.flush();
    }

    private Ride createRide(User creator, Driver driver, RideStatus status, User... passengers) {
        Set<User> passengerSet = new HashSet<>();
        passengerSet.add(creator);
        for (User passenger : passengers) {
            passengerSet.add(passenger);
        }

        Ride ride = Ride.builder()
                .creator(creator)
                .driver(driver)
                .status(status)
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .estimatedPrice(new BigDecimal("500.00"))
                .distanceKm(5.0)
                .estimatedDurationMinutes(15)
                .routeCoordinates("[[45.2551,19.8451],[45.2671,19.8335]]")
                .startedAt(status == RideStatus.IN_PROGRESS || status == RideStatus.STOP_REQUESTED ? LocalDateTime.now() : null)
                .passengers(passengerSet)
                .build();

        RideStop pickup = RideStop.builder()
                .ride(ride)
                .sequenceIndex(0)
                .address("Pickup Location")
                .latitude(45.2551)
                .longitude(19.8451)
                .build();

        RideStop dropoff = RideStop.builder()
                .ride(ride)
                .sequenceIndex(1)
                .address("Dropoff Location")
                .latitude(45.2671)
                .longitude(19.8335)
                .build();

        ride.getStops().add(pickup);
        ride.getStops().add(dropoff);

        entityManager.persist(ride);
        return ride;
    }

    // ========================================
    // findDriverActiveRides() Tests
    // ========================================

    @Test(description = "Should find ride with STOP_REQUESTED status for driver")
    public void findDriverActiveRides_WithStopRequested() {
        Ride stopRequestedRide = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findDriverActiveRides(
                driver1.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(stopRequestedRide.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
    }

    @Test(description = "Should find rides with multiple active statuses including STOP_REQUESTED")
    public void findDriverActiveRides_MultipleStatuses() {
        Ride inProgressRide = createRide(passenger1, driver1, RideStatus.IN_PROGRESS);
        Ride stopRequestedRide = createRide(passenger2, driver1, RideStatus.STOP_REQUESTED);
        Ride acceptedRide = createRide(passenger1, driver1, RideStatus.ACCEPTED);
        Ride completedRide = createRide(passenger2, driver1, RideStatus.COMPLETED);
        entityManager.flush();

        List<Ride> result = rideRepository.findDriverActiveRides(
                driver1.getId(),
                List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(3);
        assertThat(result).extracting(Ride::getStatus)
                .containsExactlyInAnyOrder(
                        RideStatus.ACCEPTED,
                        RideStatus.IN_PROGRESS,
                        RideStatus.STOP_REQUESTED
                );
        assertThat(result).extracting(Ride::getId)
                .doesNotContain(completedRide.getId());
    }

    @Test(description = "Should return empty list when driver has no active rides")
    public void findDriverActiveRides_NoActiveRides() {
        Ride completedRide = createRide(passenger1, driver1, RideStatus.COMPLETED);
        Ride cancelledRide = createRide(passenger2, driver1, RideStatus.CANCELLED);
        entityManager.flush();

        List<Ride> result = rideRepository.findDriverActiveRides(
                driver1.getId(),
                List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED)
        );

        assertThat(result).isEmpty();
    }

    @Test(description = "Should only return rides for specific driver")
    public void findDriverActiveRides_OnlySpecificDriver() {
        Ride driver1Ride = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        Ride driver2Ride = createRide(passenger2, driver2, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findDriverActiveRides(
                driver1.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDriver().getId()).isEqualTo(driver1.getId());
        assertThat(result).extracting(Ride::getId)
                .doesNotContain(driver2Ride.getId());
    }

    @Test(description = "Should return empty list for driver with no rides")
    public void findDriverActiveRides_DriverWithNoRides() {
        createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findDriverActiveRides(
                driver2.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).isEmpty();
    }

    // ========================================
    // findPassengerActiveRides() Tests
    // ========================================

    @Test(description = "Should find ride with STOP_REQUESTED status for passenger")
    public void findPassengerActiveRides_WithStopRequested() {
        Ride stopRequestedRide = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger1.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(stopRequestedRide.getId());
        assertThat(result.get(0).getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
    }

    @Test(description = "Should find rides with multiple active statuses including STOP_REQUESTED")
    public void findPassengerActiveRides_MultipleStatuses() {
        Ride inProgressRide = createRide(passenger1, driver1, RideStatus.IN_PROGRESS);
        Ride stopRequestedRide = createRide(passenger1, driver2, RideStatus.STOP_REQUESTED);
        Ride acceptedRide = createRide(passenger1, driver1, RideStatus.ACCEPTED);
        Ride scheduledRide = createRide(passenger1, driver2, RideStatus.SCHEDULED);
        Ride completedRide = createRide(passenger1, driver1, RideStatus.COMPLETED);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger1.getId(),
                List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(4);
        assertThat(result).extracting(Ride::getStatus)
                .containsExactlyInAnyOrder(
                        RideStatus.ACCEPTED,
                        RideStatus.SCHEDULED,
                        RideStatus.IN_PROGRESS,
                        RideStatus.STOP_REQUESTED
                );
        assertThat(result).extracting(Ride::getId)
                .doesNotContain(completedRide.getId());
    }

    @Test(description = "Should return empty list when passenger has no active rides")
    public void findPassengerActiveRides_NoActiveRides() {
        Ride completedRide = createRide(passenger1, driver1, RideStatus.COMPLETED);
        Ride cancelledRide = createRide(passenger1, driver2, RideStatus.CANCELLED);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger1.getId(),
                List.of(RideStatus.ACCEPTED, RideStatus.IN_PROGRESS, RideStatus.STOP_REQUESTED)
        );

        assertThat(result).isEmpty();
    }

    @Test(description = "Should only return rides for specific passenger")
    public void findPassengerActiveRides_OnlySpecificPassenger() {
        Ride passenger1Ride = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        Ride passenger2Ride = createRide(passenger2, driver2, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger1.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPassengers())
                .anyMatch(p -> p.getId().equals(passenger1.getId()));
        assertThat(result).extracting(Ride::getId)
                .doesNotContain(passenger2Ride.getId());
    }

    @Test(description = "Should find ride when passenger is not creator but is in passengers list")
    public void findPassengerActiveRides_PassengerNotCreator() {
        Ride ride = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED, passenger2);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger2.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(ride.getId());
        assertThat(result.get(0).getCreator().getId()).isEqualTo(passenger1.getId());
        assertThat(result.get(0).getPassengers())
                .anyMatch(p -> p.getId().equals(passenger2.getId()));
    }

    @Test(description = "Should return empty list for passenger with no rides")
    public void findPassengerActiveRides_PassengerWithNoRides() {
        createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();

        List<Ride> result = rideRepository.findPassengerActiveRides(
                passenger2.getId(),
                List.of(RideStatus.STOP_REQUESTED)
        );

        assertThat(result).isEmpty();
    }

    // ========================================
    // General Repository Tests for Early Stoppage Scenarios
    // ========================================

    @Test(description = "Should persist ride with STOP_REQUESTED status")
    public void saveRide_WithStopRequestedStatus() {
        Ride ride = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();
        entityManager.clear();

        Ride found = rideRepository.findById(ride.getId()).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
        assertThat(found.getDriver().getId()).isEqualTo(driver1.getId());
        assertThat(found.getPassengers()).hasSize(1);
    }

    @Test(description = "Should update ride status from IN_PROGRESS to STOP_REQUESTED")
    public void updateRideStatus_ToStopRequested() {
        Ride ride = createRide(passenger1, driver1, RideStatus.IN_PROGRESS);
        entityManager.flush();
        entityManager.clear();

        Ride foundRide = rideRepository.findById(ride.getId()).orElseThrow();
        foundRide.setStatus(RideStatus.STOP_REQUESTED);
        rideRepository.save(foundRide);
        entityManager.flush();
        entityManager.clear();

        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
    }

    @Test(description = "Should update ride with all early stop changes: status, stop location, distance, and price")
    public void updateRide_EarlyStopScenario() {
        // Given
        Ride ride = createRide(passenger1, driver1, RideStatus.STOP_REQUESTED);
        entityManager.flush();
        entityManager.clear();

        // When - Simulate complete early stop update
        Ride foundRide = rideRepository.findById(ride.getId()).orElseThrow();

        // Update status and completion time
        foundRide.setStatus(RideStatus.COMPLETED);
        foundRide.setCompletedAt(LocalDateTime.now());

        // Update stop location
        RideStop lastStop = foundRide.getStops().get(foundRide.getStops().size() - 1);
        lastStop.setAddress("New Stop Location");
        lastStop.setLatitude(45.2600);
        lastStop.setLongitude(19.8400);

        // Update distance and price
        foundRide.setDistanceKm(3.5);
        foundRide.setEstimatedPrice(new BigDecimal("420.00"));

        rideRepository.save(foundRide);
        entityManager.flush();
        entityManager.clear();

        // Then - Verify all changes persisted
        Ride updatedRide = rideRepository.findById(ride.getId()).orElseThrow();

        // Verify status update
        assertThat(updatedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(updatedRide.getCompletedAt()).isNotNull();

        // Verify stop location update
        RideStop updatedStop = updatedRide.getStops().get(updatedRide.getStops().size() - 1);
        assertThat(updatedStop.getAddress()).isEqualTo("New Stop Location");
        assertThat(updatedStop.getLatitude()).isEqualTo(45.2600);
        assertThat(updatedStop.getLongitude()).isEqualTo(19.8400);

        // Verify distance and price update
        assertThat(updatedRide.getDistanceKm()).isEqualTo(3.5);
        assertThat(updatedRide.getEstimatedPrice()).isEqualTo(new BigDecimal("420.00"));
    }
}
