package com.pekara.service;

import com.pekara.constant.RideStatus;
import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.dto.response.OrderRideResponse;
import com.pekara.exception.ActiveRideConflictException;
import com.pekara.exception.InvalidScheduleTimeException;
import com.pekara.exception.NoDriversAvailableException;
import com.pekara.exception.UserBlockedException;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.Ride;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.InconsistencyReportRepository;
import com.pekara.repository.RideRatingRepository;
import com.pekara.repository.RideRepository;
import com.pekara.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for RideServiceImpl - ride ordering (orderRide) functionality.
 */
@org.testng.annotations.Listeners(MockitoTestNGListener.class)
public class RideServiceImplOrderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RideRatingRepository rideRatingRepository;

    @Mock
    private InconsistencyReportRepository inconsistencyReportRepository;

    @Mock
    private DriverStateRepository driverStateRepository;

    @Mock
    private EntityManager entityManager;

    @Mock
    private RideEstimationService rideEstimationService;

    @Mock
    private DriverMatchingService driverMatchingService;

    @Mock
    private DriverStateManagementService driverStateManagementService;

    @Mock
    private RideWorkLogService rideWorkLogService;

    @Mock
    private RideNotificationService rideNotificationService;

    @Mock
    private RoutingService routingService;

    @InjectMocks
    private RideServiceImpl rideService;

    private User creator;
    private Driver driver;
    private DriverState driverState;
    private OrderRideRequest request;
    private static final String CREATOR_EMAIL = "creator@test.com";

    @BeforeMethod
    public void setUp() {
        creator = User.builder()
                .id(1L)
                .email(CREATOR_EMAIL)
                .firstName("Creator")
                .lastName("User")
                .role(UserRole.PASSENGER)
                .blocked(false)
                .build();

        driver = new Driver();
        driver.setId(2L);
        driver.setEmail("driver@test.com");
        driver.setVehicleType("SEDAN");

        driverState = new DriverState();
        driverState.setId(driver.getId());
        driverState.setDriver(driver);
        driverState.setOnline(true);
        driverState.setBusy(false);

        LocationPointDto pickup = LocationPointDto.builder().address("A").latitude(45.25).longitude(19.84).build();
        LocationPointDto dropoff = LocationPointDto.builder().address("B").latitude(45.27).longitude(19.85).build();
        request = OrderRideRequest.builder()
                .pickup(pickup)
                .dropoff(dropoff)
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .scheduledAt(null)
                .build();
    }

    @Test(description = "Should return OrderRideResponse and save ride when order succeeds")
    public void orderRide_Success_ReturnsResponseAndSavesRide() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(creator.getId(), List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS)))
                .thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20, List.of()));
        when(rideEstimationService.calculatePrice(eq("SEDAN"), eq(10.0))).thenReturn(new BigDecimal("500.00"));
        when(rideEstimationService.roundKm(10.0)).thenReturn(10.0);
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[]");
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(2L);
        when(driverStateRepository.findByDriverIdForUpdate(2L)).thenReturn(Optional.of(driverState));

        Ride savedRide = Ride.builder().id(100L).status(RideStatus.ACCEPTED).estimatedPrice(new BigDecimal("500.00")).build();
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        OrderRideResponse response = rideService.orderRide(CREATOR_EMAIL, request);

        assertThat(response.getRideId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(RideStatus.ACCEPTED.name());
        assertThat(response.getMessage()).contains("ordered successfully");
        assertThat(response.getEstimatedPrice()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(response.getAssignedDriverEmail()).isEqualTo("driver@test.com");

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        Ride captured = rideCaptor.getValue();
        assertThat(captured.getCreator()).isEqualTo(creator);
        assertThat(captured.getDriver()).isEqualTo(driver);
        assertThat(captured.getStatus()).isEqualTo(RideStatus.ACCEPTED);
        assertThat(captured.getStops()).hasSize(2);

        verify(driverStateManagementService).markDriverBusy(eq(2L), eq(20), eq(45.27), eq(19.85));
        verify(rideWorkLogService).createWorkLogForRide(eq(100L), eq(2L), any(LocalDateTime.class));
        verify(rideNotificationService).sendRideOrderNotifications(eq("driver@test.com"), eq(CREATOR_EMAIL), eq(100L), eq("ACCEPTED"), any(), any());
    }

    @Test(description = "Should set SCHEDULED and call setNextScheduledRide when scheduledAt is set")
    public void orderRide_Scheduled_SetsStatusAndCallsSetNextScheduledRide() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusHours(2);
        request.setScheduledAt(scheduledAt);

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20, List.of()));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("500.00"));
        when(rideEstimationService.roundKm(anyDouble())).thenReturn(10.0);
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[]");
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(2L);
        when(driverStateRepository.findByDriverIdForUpdate(2L)).thenReturn(Optional.of(driverState));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(101L);
            return r;
        });

        OrderRideResponse response = rideService.orderRide(CREATOR_EMAIL, request);

        assertThat(response.getStatus()).isEqualTo(RideStatus.SCHEDULED.name());
        assertThat(response.getScheduledAt()).isEqualTo(scheduledAt);
        verify(driverStateManagementService).setNextScheduledRide(2L, scheduledAt);
        verify(rideWorkLogService, never()).createWorkLogForRide(anyLong(), anyLong(), any());
    }

    @Test(description = "Should throw UserBlockedException when creator is blocked")
    public void orderRide_CreatorBlocked_ThrowsUserBlockedException() {
        creator.setBlocked(true);
        creator.setBlockedNote("Abuse");
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(UserBlockedException.class)
                .hasMessageContaining("blocked");
        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw ActiveRideConflictException when creator has active ride")
    public void orderRide_ActiveRideExists_ThrowsActiveRideConflictException() {
        Ride activeRide = Ride.builder().id(50L).status(RideStatus.ACCEPTED).build();
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(creator.getId(), List.of(RideStatus.ACCEPTED, RideStatus.SCHEDULED, RideStatus.IN_PROGRESS)))
                .thenReturn(List.of(activeRide));

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(ActiveRideConflictException.class)
                .hasMessageContaining("active ride");
        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw when scheduledAt is in the past")
    public void orderRide_ScheduledInPast_ThrowsInvalidScheduleTimeException() {
        request.setScheduledAt(LocalDateTime.now().minusMinutes(10));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(InvalidScheduleTimeException.class)
                .hasMessageContaining("future");
    }

    @Test(description = "Should throw when scheduledAt is more than 5 hours ahead")
    public void orderRide_ScheduledMoreThan5h_ThrowsInvalidScheduleTimeException() {
        request.setScheduledAt(LocalDateTime.now().plusHours(6));
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(InvalidScheduleTimeException.class)
                .hasMessageContaining("5 hours");
    }

    @Test(description = "Should throw and send rejection when no driver available")
    public void orderRide_NoDriver_ThrowsAndSendsRejection() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20, List.of()));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("500.00"));
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[]");
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(null);

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(NoDriversAvailableException.class)
                .hasMessageContaining("no active drivers");
        verify(rideNotificationService).sendRejectionNotification(CREATOR_EMAIL, "No active drivers available");
        verify(rideRepository, never()).save(any());
    }

    @Test(description = "Should throw when driver state not found after selection")
    public void orderRide_DriverStateNotFoundAfterSelection_ThrowsNoDriversAvailable() {
        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20, List.of()));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("500.00"));
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(2L);
        when(driverStateRepository.findByDriverIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> rideService.orderRide(CREATOR_EMAIL, request))
                .isInstanceOf(NoDriversAvailableException.class)
                .hasMessageContaining("Driver became unavailable");
    }

    @Test(description = "Should add additional passengers from passengerEmails")
    public void orderRide_WithPassengerEmails_AddsPassengersToRide() {
        User otherPassenger = User.builder().id(3L).email("other@test.com").role(UserRole.PASSENGER).build();
        request.setPassengerEmails(List.of("other@test.com"));

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherPassenger));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(10.0, 20, List.of()));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("500.00"));
        when(rideEstimationService.roundKm(anyDouble())).thenReturn(10.0);
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[]");
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(2L);
        when(driverStateRepository.findByDriverIdForUpdate(2L)).thenReturn(Optional.of(driverState));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(102L);
            return r;
        });

        rideService.orderRide(CREATOR_EMAIL, request);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        assertThat(rideCaptor.getValue().getPassengers()).containsExactlyInAnyOrder(creator, otherPassenger);
    }

    @Test(description = "Should build stops in order: pickup, optional stops, dropoff")
    public void orderRide_WithStops_BuildsStopsInOrder() {
        LocationPointDto stop1 = LocationPointDto.builder().address("Stop1").latitude(45.26).longitude(19.845).build();
        request.setStops(List.of(stop1));

        when(userRepository.findByEmail(CREATOR_EMAIL)).thenReturn(Optional.of(creator));
        when(rideRepository.findPassengerActiveRides(anyLong(), any())).thenReturn(List.of());
        when(rideEstimationService.calculateRouteWithStops(any(), any(), any()))
                .thenReturn(new RideEstimationService.RouteData(12.0, 25, List.of()));
        when(rideEstimationService.calculatePrice(any(), anyDouble())).thenReturn(new BigDecimal("600.00"));
        when(rideEstimationService.roundKm(anyDouble())).thenReturn(12.0);
        when(rideEstimationService.serializeRouteCoordinates(any())).thenReturn("[]");
        when(driverMatchingService.selectDriverIdForRide(any(), any())).thenReturn(2L);
        when(driverStateRepository.findByDriverIdForUpdate(2L)).thenReturn(Optional.of(driverState));
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride r = inv.getArgument(0);
            r.setId(103L);
            return r;
        });

        rideService.orderRide(CREATOR_EMAIL, request);

        ArgumentCaptor<Ride> rideCaptor = ArgumentCaptor.forClass(Ride.class);
        verify(rideRepository).save(rideCaptor.capture());
        List<com.pekara.model.RideStop> stops = rideCaptor.getValue().getStops();
        assertThat(stops).hasSize(3);
        assertThat(stops.get(0).getAddress()).isEqualTo("A");
        assertThat(stops.get(1).getAddress()).isEqualTo("Stop1");
        assertThat(stops.get(2).getAddress()).isEqualTo("B");
    }
}
