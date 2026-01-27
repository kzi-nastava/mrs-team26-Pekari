package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.Ride;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.DriverWorkLogRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RideServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private DriverStateRepository driverStateRepository;

    @Mock
    private DriverWorkLogRepository driverWorkLogRepository;

    @Mock
    private MailService mailService;

    @Mock
    private RoutingService routingService;

    @InjectMocks
    private RideServiceImpl rideService;

    private User passenger;
    private Driver driver;
    private Ride scheduledRide;
    private Ride acceptedRide;
    private Ride inProgressRide;
    private DriverState driverState;

    @BeforeEach
    void setUp() {
        passenger = User.builder()
                .id(1L)
                .email("passenger@example.com")
                .firstName("John")
                .lastName("Passenger")
                .role(UserRole.PASSENGER)
                .isActive(true)
                .build();

        driver = Driver.builder()
                .id(2L)
                .email("driver@example.com")
                .firstName("Jane")
                .lastName("Driver")
                .role(UserRole.DRIVER)
                .isActive(true)
                .licenseNumber("ABC123")
                .vehicleType("STANDARD")
                .build();

        driverState = DriverState.builder()
                .driver(driver)
                .online(true)
                .busy(false)
                .latitude(45.2671)
                .longitude(19.8335)
                .build();

        scheduledRide = Ride.builder()
                .id(1L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusHours(1))
                .passengers(new HashSet<>())
                .build();
        scheduledRide.getPassengers().add(passenger);

        acceptedRide = Ride.builder()
                .id(2L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.ACCEPTED)
                .scheduledAt(null)
                .passengers(new HashSet<>())
                .build();
        acceptedRide.getPassengers().add(passenger);

        inProgressRide = Ride.builder()
                .id(3L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.IN_PROGRESS)
                .scheduledAt(null)
                .startedAt(LocalDateTime.now().minusMinutes(5))
                .passengers(new HashSet<>())
                .build();
        inProgressRide.getPassengers().add(passenger);
    }

    // ========== DRIVER CANCELLATION TESTS ==========

    @Test
    @DisplayName("Driver can cancel ACCEPTED ride with reason")
    void driverCancelAcceptedRide_Success() {
        // Given
        String reason = "Passenger not found at pickup location";
        when(rideRepository.findById(acceptedRide.getId())).thenReturn(Optional.of(acceptedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(acceptedRide);
        when(driverStateRepository.findById(driver.getId())).thenReturn(Optional.of(driverState));
        when(driverStateRepository.save(any(DriverState.class))).thenReturn(driverState);
        when(driverWorkLogRepository.findByRide(any(Ride.class))).thenReturn(Optional.empty());

        // When
        rideService.cancelRide(acceptedRide.getId(), driver.getEmail(), reason);

        // Then
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride savedRide = rideCaptor.getValue();

        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(savedRide.getCancellationReason()).isEqualTo(reason);
        assertThat(savedRide.getCancelledBy()).isEqualTo("DRIVER");
        assertThat(savedRide.getCancelledAt()).isNotNull();
        assertThat(savedRide.getCancelledAt()).isBeforeOrEqualTo(LocalDateTime.now());
    }

    @Test
    @DisplayName("Driver can cancel SCHEDULED ride with reason")
    void driverCancelScheduledRide_Success() {
        // Given
        String reason = "Driver health issue - must end shift";
        when(rideRepository.findById(scheduledRide.getId())).thenReturn(Optional.of(scheduledRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(scheduledRide);
        when(driverStateRepository.findById(driver.getId())).thenReturn(Optional.of(driverState));
        when(driverStateRepository.save(any(DriverState.class))).thenReturn(driverState);
        when(driverWorkLogRepository.findByRide(any(Ride.class))).thenReturn(Optional.empty());

        // When
        rideService.cancelRide(scheduledRide.getId(), driver.getEmail(), reason);

        // Then
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride savedRide = rideCaptor.getValue();

        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(savedRide.getCancellationReason()).isEqualTo(reason);
        assertThat(savedRide.getCancelledBy()).isEqualTo("DRIVER");
        assertThat(savedRide.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("Driver CANNOT cancel IN_PROGRESS ride (passengers already in vehicle)")
    void driverCancelInProgressRide_ThrowsException() {
        // Given
        String reason = "Cannot continue";
        when(rideRepository.findById(inProgressRide.getId())).thenReturn(Optional.of(inProgressRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(inProgressRide.getId(), driver.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel ride that is already in progress");

        verify(rideRepository).findById(inProgressRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // ========== PASSENGER CANCELLATION TESTS ==========

    @Test
    @DisplayName("Passenger can cancel SCHEDULED ride 10+ minutes before start")
    void passengerCancelScheduledRide_MoreThan10MinutesBeforeStart_Success() {
        // Given
        Ride futureRide = Ride.builder()
                .id(4L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusMinutes(15)) // 15 minutes in future
                .passengers(new HashSet<>())
                .build();
        futureRide.getPassengers().add(passenger);

        String reason = "No longer need the ride";
        when(rideRepository.findById(futureRide.getId())).thenReturn(Optional.of(futureRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(futureRide);
        when(driverStateRepository.findById(driver.getId())).thenReturn(Optional.of(driverState));
        when(driverStateRepository.save(any(DriverState.class))).thenReturn(driverState);
        when(driverWorkLogRepository.findByRide(any(Ride.class))).thenReturn(Optional.empty());

        // When
        rideService.cancelRide(futureRide.getId(), passenger.getEmail(), reason);

        // Then
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride savedRide = rideCaptor.getValue();

        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(savedRide.getCancellationReason()).isEqualTo(reason);
        assertThat(savedRide.getCancelledBy()).isEqualTo("PASSENGER");
        assertThat(savedRide.getCancelledAt()).isNotNull();
    }

    @Test
    @DisplayName("Passenger CANNOT cancel SCHEDULED ride less than 10 minutes before start")
    void passengerCancelScheduledRide_LessThan10MinutesBeforeStart_ThrowsException() {
        // Given
        Ride soonRide = Ride.builder()
                .id(5L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now().plusMinutes(5)) // Only 5 minutes in future
                .passengers(new HashSet<>())
                .build();
        soonRide.getPassengers().add(passenger);

        String reason = "Changed my mind";
        when(rideRepository.findById(soonRide.getId())).thenReturn(Optional.of(soonRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(soonRide.getId(), passenger.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel scheduled ride less than 10 minutes before start time");

        verify(rideRepository).findById(soonRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Passenger CANNOT cancel immediate ride that is IN_PROGRESS")
    void passengerCancelInProgressImmediateRide_ThrowsException() {
        // Given
        String reason = "Want to exit the vehicle";
        when(rideRepository.findById(inProgressRide.getId())).thenReturn(Optional.of(inProgressRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(inProgressRide.getId(), passenger.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel ride that is already in progress");

        verify(rideRepository).findById(inProgressRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Passenger CAN cancel immediate ACCEPTED ride (not yet in progress)")
    void passengerCancelAcceptedImmediateRide_Success() {
        // Given
        String reason = "Changed my mind";
        when(rideRepository.findById(acceptedRide.getId())).thenReturn(Optional.of(acceptedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(acceptedRide);
        when(driverStateRepository.findById(driver.getId())).thenReturn(Optional.of(driverState));
        when(driverStateRepository.save(any(DriverState.class))).thenReturn(driverState);
        when(driverWorkLogRepository.findByRide(any(Ride.class))).thenReturn(Optional.empty());

        // When
        rideService.cancelRide(acceptedRide.getId(), passenger.getEmail(), reason);

        // Then
        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride savedRide = rideCaptor.getValue();

        assertThat(savedRide.getStatus()).isEqualTo(RideStatus.CANCELLED);
        assertThat(savedRide.getCancellationReason()).isEqualTo(reason);
        assertThat(savedRide.getCancelledBy()).isEqualTo("PASSENGER");
        assertThat(savedRide.getCancelledAt()).isNotNull();
    }

    // ========== AUTHORIZATION TESTS ==========

    @Test
    @DisplayName("Unauthorized user CANNOT cancel ride")
    void unauthorizedUserCancelRide_ThrowsException() {
        // Given
        User stranger = User.builder()
                .id(99L)
                .email("stranger@example.com")
                .build();

        String reason = "Test reason";
        when(rideRepository.findById(acceptedRide.getId())).thenReturn(Optional.of(acceptedRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(acceptedRide.getId(), stranger.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You are not authorized to cancel this ride");

        verify(rideRepository).findById(acceptedRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Cannot cancel already COMPLETED ride")
    void cancelCompletedRide_ThrowsException() {
        // Given
        Ride completedRide = Ride.builder()
                .id(6L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.COMPLETED)
                .completedAt(LocalDateTime.now().minusHours(1))
                .passengers(new HashSet<>())
                .build();
        completedRide.getPassengers().add(passenger);

        String reason = "Test reason";
        when(rideRepository.findById(completedRide.getId())).thenReturn(Optional.of(completedRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(completedRide.getId(), passenger.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ride cannot be cancelled in current status: COMPLETED");

        verify(rideRepository).findById(completedRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    @Test
    @DisplayName("Cannot cancel already CANCELLED ride")
    void cancelAlreadyCancelledRide_ThrowsException() {
        // Given
        Ride cancelledRide = Ride.builder()
                .id(7L)
                .creator(passenger)
                .driver(driver)
                .status(RideStatus.CANCELLED)
                .cancelledAt(LocalDateTime.now().minusMinutes(10))
                .passengers(new HashSet<>())
                .build();
        cancelledRide.getPassengers().add(passenger);

        String reason = "Test reason";
        when(rideRepository.findById(cancelledRide.getId())).thenReturn(Optional.of(cancelledRide));

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(cancelledRide.getId(), passenger.getEmail(), reason))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ride cannot be cancelled in current status: CANCELLED");

        verify(rideRepository).findById(cancelledRide.getId());
        verify(rideRepository, never()).save(any(Ride.class));
    }

    // ========== DRIVER STATE UPDATE TESTS ==========

    @Test
    @DisplayName("Cancellation should free driver state (busy=false, clear times)")
    void cancelRide_ShouldFreeDriverState() {
        // Given
        driverState.setBusy(true);
        driverState.setCurrentRideEndsAt(LocalDateTime.now().plusMinutes(20));
        driverState.setCurrentRideEndLatitude(45.123);
        driverState.setCurrentRideEndLongitude(19.456);
        driverState.setNextScheduledRideAt(LocalDateTime.now().plusHours(2));

        String reason = "Test cancellation";
        when(rideRepository.findById(acceptedRide.getId())).thenReturn(Optional.of(acceptedRide));
        when(rideRepository.save(any(Ride.class))).thenReturn(acceptedRide);
        when(driverStateRepository.findById(driver.getId())).thenReturn(Optional.of(driverState));
        when(driverStateRepository.save(any(DriverState.class))).thenReturn(driverState);
        when(driverWorkLogRepository.findByRide(any(Ride.class))).thenReturn(Optional.empty());

        // When
        rideService.cancelRide(acceptedRide.getId(), driver.getEmail(), reason);

        // Then
        ArgumentCaptor<DriverState> driverStateCaptor = ArgumentCaptor.forClass(DriverState.class);
        verify(driverStateRepository).save(driverStateCaptor.capture());
        DriverState savedDriverState = driverStateCaptor.getValue();

        assertThat(savedDriverState.getBusy()).isFalse();
        assertThat(savedDriverState.getCurrentRideEndsAt()).isNull();
        assertThat(savedDriverState.getCurrentRideEndLatitude()).isNull();
        assertThat(savedDriverState.getCurrentRideEndLongitude()).isNull();
        assertThat(savedDriverState.getNextScheduledRideAt()).isNull();
    }

    @Test
    @DisplayName("Ride not found - should throw exception")
    void cancelRide_RideNotFound_ThrowsException() {
        // Given
        Long nonExistentRideId = 999L;
        when(rideRepository.findById(nonExistentRideId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> rideService.cancelRide(nonExistentRideId, passenger.getEmail(), "reason"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Ride not found");

        verify(rideRepository).findById(nonExistentRideId);
        verify(rideRepository, never()).save(any(Ride.class));
    }
}
