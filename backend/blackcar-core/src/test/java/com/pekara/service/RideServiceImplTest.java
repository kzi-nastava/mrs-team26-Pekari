package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.model.Driver;
import com.pekara.model.Ride;
import com.pekara.model.RideStop;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.RideRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RideServiceImpl - Early Ride Stoppage functionality
 * Tests the requestStopRide() and stopRideEarly() methods
 */
@Listeners(MockitoTestNGListener.class)
public class RideServiceImplTest {

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverStateManagementService driverStateManagementService;

    @Mock
    private RideWorkLogService rideWorkLogService;

    @Mock
    private RoutingService routingService;

    @Mock
    private RideEstimationService rideEstimationService;

    @Mock
    private RideNotificationService rideNotificationService;

    @InjectMocks
    private RideServiceImpl rideService;

    private User passenger;
    private User anotherPassenger;
    private Driver driver;
    private Ride ride;
    private LocationPointDto newStopLocation;

    @BeforeMethod
    public void setUp() {
        passenger = User.builder()
                .id(1L)
                .email("passenger@test.com")
                .firstName("John")
                .lastName("Doe")
                .role(UserRole.PASSENGER)
                .build();

        anotherPassenger = User.builder()
                .id(2L)
                .email("another@test.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(UserRole.PASSENGER)
                .build();

        driver = Driver.builder()
                .id(3L)
                .email("driver@test.com")
                .firstName("Bob")
                .lastName("Driver")
                .role(UserRole.DRIVER)
                .vehicleType("SEDAN")
                .build();

        Set<User> passengers = new HashSet<>();
        passengers.add(passenger);

        List<RideStop> stops = new ArrayList<>();
        stops.add(RideStop.builder()
                .id(1L)
                .sequenceIndex(0)
                .address("Pickup Location")
                .latitude(45.2551)
                .longitude(19.8451)
                .build());
        stops.add(RideStop.builder()
                .id(2L)
                .sequenceIndex(1)
                .address("Dropoff Location")
                .latitude(45.2671)
                .longitude(19.8335)
                .build());

        ride = Ride.builder()
                .id(1L)
                .creator(passenger)
                .driver(driver)
                .passengers(passengers)
                .status(RideStatus.IN_PROGRESS)
                .vehicleType("SEDAN")
                .estimatedPrice(new BigDecimal("500.00"))
                .distanceKm(5.0)
                .estimatedDurationMinutes(15)
                .routeCoordinates("[[45.2551,19.8451],[45.2671,19.8335]]")
                .stops(stops)
                .startedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        newStopLocation = LocationPointDto.builder()
                .address("New Stop Location")
                .latitude(45.2600)
                .longitude(19.8400)
                .build();
    }

    // ========================================
    // requestStopRide() Tests
    // ========================================

    @Test(description = "Should successfully request stop on IN_PROGRESS ride")
    public void requestStopRide_Success() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);

        rideService.requestStopRide(1L, "passenger@test.com");

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getValue();
        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.STOP_REQUESTED);
        verify(rideRepository).findById(1L);
    }

    @Test(description = "Should throw exception when ride not found")
    public void requestStopRide_RideNotFound() {
        when(rideRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.requestStopRide(999L, "passenger@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ride not found");

        verify(rideRepository, never()).save(any());
    }

    @DataProvider(name = "invalidRequestStopStatuses")
    public Object[][] invalidRequestStopStatuses() {
        return new Object[][] {
            {RideStatus.ACCEPTED},
            {RideStatus.COMPLETED},
            {RideStatus.CANCELLED},
            {RideStatus.SCHEDULED}
        };
    }

    @Test(dataProvider = "invalidRequestStopStatuses",
          description = "Should throw exception when ride status is not IN_PROGRESS")
    public void requestStopRide_InvalidStatus(RideStatus status) {
        ride.setStatus(status);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.requestStopRide(1L, "passenger@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only in-progress rides can be stopped");

        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw exception when user is not a passenger on ride")
    public void requestStopRide_Unauthorized() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.requestStopRide(1L, "stranger@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are not a passenger on this ride");

        verify(rideRepository, never()).save(any());
    }

    // ========================================
    // stopRideEarly() Tests
    // ========================================

    @DataProvider(name = "validStopEarlyStatuses")
    public Object[][] validStopEarlyStatuses() {
        return new Object[][] {
            {RideStatus.IN_PROGRESS},
            {RideStatus.STOP_REQUESTED}
        };
    }

    @Test(dataProvider = "validStopEarlyStatuses",
          description = "Should successfully stop ride early from valid statuses")
    public void stopRideEarly_Success(RideStatus status) {
        ride.setStatus(status);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);
        when(routingService.calculateActualDistanceFromRoute(anyString(), any(LocationPointDto.class)))
                .thenReturn(3.5);
        when(rideEstimationService.calculatePrice(eq("SEDAN"), eq(3.5)))
                .thenReturn(new BigDecimal("420.00"));
        when(rideEstimationService.roundKm(3.5)).thenReturn(3.5);

        rideService.stopRideEarly(1L, "driver@test.com", newStopLocation);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getValue();
        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(savedRide.getCompletedAt()).isNotNull();
        assertThat(savedRide.getDistanceKm()).isEqualTo(3.5);
        assertThat(savedRide.getEstimatedPrice()).isEqualTo(new BigDecimal("420.00"));

        RideStop lastStop = savedRide.getStops().get(savedRide.getStops().size() - 1);
        assertThat(lastStop.getAddress()).isEqualTo("New Stop Location");
        assertThat(lastStop.getLatitude()).isEqualTo(45.2600);
        assertThat(lastStop.getLongitude()).isEqualTo(19.8400);

        verify(driverStateManagementService).releaseDriverAfterRide(3L);
        verify(rideWorkLogService).completeWorkLog(eq(1L), any(LocalDateTime.class));
    }

    @Test(description = "Should throw exception when ride not found")
    public void stopRideEarly_RideNotFound() {
        when(rideRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.stopRideEarly(999L, "driver@test.com", newStopLocation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ride not found");

        verify(rideRepository, never()).save(any());
    }

    // ========================================
    // completeRide() Tests
    // ========================================

    @Test(description = "Should successfully complete ride")
    public void completeRide_Success() {
        ride.setStatus(RideStatus.IN_PROGRESS);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);

        rideService.completeRide(1L, "driver@test.com");

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getValue();
        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.COMPLETED);
        assertThat(savedRide.getCompletedAt()).isNotNull();

        verify(driverStateManagementService).releaseDriverAfterRide(3L);
        verify(rideWorkLogService).completeWorkLog(eq(1L), any(LocalDateTime.class));
        verify(rideNotificationService).sendRideCompletionNotifications(eq(1L), anyList(), any(BigDecimal.class));
    }

    @Test(description = "Should throw exception when ride not found in completeRide")
    public void completeRide_RideNotFound() {
        when(rideRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.completeRide(999L, "driver@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ride not found");

        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw exception when driver is not assigned to ride")
    public void completeRide_UnauthorizedDriver() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide(1L, "wrong_driver@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are not the assigned driver for this ride");

        verify(rideRepository, never()).save(any());
    }

    @DataProvider(name = "invalidCompleteStatuses")
    public Object[][] invalidCompleteStatuses() {
        return new Object[][] {
            {RideStatus.ACCEPTED},
            {RideStatus.COMPLETED},
            {RideStatus.CANCELLED},
            {RideStatus.SCHEDULED},
            {RideStatus.STOP_REQUESTED} // completeRide only allows IN_PROGRESS, stopRideEarly handles STOP_REQUESTED
        };
    }

    @Test(dataProvider = "invalidCompleteStatuses",
          description = "Should throw exception when ride status is not IN_PROGRESS for completeRide")
    public void completeRide_InvalidStatus(RideStatus status) {
        ride.setStatus(status);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.completeRide(1L, "driver@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only in-progress rides can be completed");

        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw exception when user is not the assigned driver")
    public void stopRideEarly_Unauthorized() {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.stopRideEarly(1L, "wrong@driver.com", newStopLocation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("You are not the assigned driver for this ride");

        verify(rideRepository, never()).save(any());
    }

    @DataProvider(name = "invalidStopEarlyStatuses")
    public Object[][] invalidStopEarlyStatuses() {
        return new Object[][] {
            {RideStatus.ACCEPTED},
            {RideStatus.COMPLETED},
            {RideStatus.CANCELLED}
        };
    }

    @Test(dataProvider = "invalidStopEarlyStatuses",
          description = "Should throw exception when ride status is invalid for early stop")
    public void stopRideEarly_InvalidStatus(RideStatus status) {
        ride.setStatus(status);
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.stopRideEarly(1L, "driver@test.com", newStopLocation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only in-progress or stop-requested rides can be stopped early");

        verify(rideRepository, never()).save(any());
    }

    @DataProvider(name = "invalidStopLocations")
    public Object[][] invalidStopLocations() {
        return new Object[][] {
            {null},
            {LocationPointDto.builder().latitude(45.2600).longitude(19.8400).address(null).build()},
            {LocationPointDto.builder().latitude(45.2600).longitude(19.8400).address("   ").build()}
        };
    }

    @Test(dataProvider = "invalidStopLocations",
          description = "Should throw exception when stop location is invalid")
    public void stopRideEarly_InvalidStopLocation(LocationPointDto invalidLocation) {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));

        assertThatThrownBy(() -> rideService.stopRideEarly(1L, "driver@test.com", invalidLocation))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Valid stop location is required");

        verify(rideRepository, never()).save(any());
    }

    @DataProvider(name = "invalidRoutingDistances")
    public Object[][] invalidRoutingDistances() {
        return new Object[][] {
            {null},
            {0.0}
        };
    }

    @Test(dataProvider = "invalidRoutingDistances",
          description = "Should use fallback calculation when routing service returns invalid distance")
    public void stopRideEarly_FallbackDistanceCalculation(Double invalidDistance) {
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);
        when(routingService.calculateActualDistanceFromRoute(anyString(), any(LocationPointDto.class)))
                .thenReturn(invalidDistance);
        when(rideEstimationService.calculatePrice(eq("SEDAN"), anyDouble()))
                .thenReturn(new BigDecimal("300.00"));
        when(rideEstimationService.roundKm(anyDouble())).thenAnswer(invocation -> invocation.getArgument(0));

        rideService.stopRideEarly(1L, "driver@test.com", newStopLocation);

        verify(rideRepository).save(any(Ride.class));
        verify(rideEstimationService).calculatePrice(eq("SEDAN"), anyDouble());
    }

    @Test(description = "Should use ride distance when routing fails and stops are empty")
    public void stopRideEarly_EmptyStops_UsesRideDistance() {
        ride.setStops(new ArrayList<>());
        when(rideRepository.findById(1L)).thenReturn(Optional.of(ride));
        when(rideRepository.save(any(Ride.class))).thenReturn(ride);
        when(routingService.calculateActualDistanceFromRoute(anyString(), any(LocationPointDto.class)))
                .thenReturn(0.0);
        when(rideEstimationService.calculatePrice(eq("SEDAN"), eq(5.0)))
                .thenReturn(new BigDecimal("600.00"));
        when(rideEstimationService.roundKm(5.0)).thenReturn(5.0);

        rideService.stopRideEarly(1L, "driver@test.com", newStopLocation);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());

        Ride savedRide = rideCaptor.getValue();
        assertThat(savedRide.getDistanceKm()).isEqualTo(5.0);
        verify(rideEstimationService).calculatePrice(eq("SEDAN"), eq(5.0));
    }

}
