package com.pekara.service;

import com.pekara.dto.common.LocationPointDto;
import com.pekara.dto.request.OrderRideRequest;
import com.pekara.exception.NoActiveDriversException;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.model.DriverWorkLog;
import com.pekara.repository.DriverStateRepository;
import com.pekara.repository.DriverWorkLogRepository;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.testng.MockitoTestNGListener;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DriverMatchingServiceImpl - ride ordering driver selection and 8h work limit.
 */
@Listeners(MockitoTestNGListener.class)
public class DriverMatchingServiceImplTest {

    @Mock
    private DriverStateRepository driverStateRepository;

    @Mock
    private DriverWorkLogRepository driverWorkLogRepository;

    @InjectMocks
    private DriverMatchingServiceImpl driverMatchingService;

    private OrderRideRequest request;
    private LocalDateTime now;
    private LocationPointDto pickup;

    @BeforeMethod
    public void setUp() {
        now = LocalDateTime.of(2025, 2, 18, 12, 0);
        pickup = LocationPointDto.builder()
                .address("Pickup")
                .latitude(45.25)
                .longitude(19.84)
                .build();
        request = OrderRideRequest.builder()
                .pickup(pickup)
                .dropoff(LocationPointDto.builder().address("Dropoff").latitude(45.27).longitude(19.85).build())
                .vehicleType("SEDAN")
                .babyTransport(false)
                .petTransport(false)
                .build();
    }

    @Test(description = "Should throw when no online drivers")
    public void selectDriverIdForRide_NoOnlineDrivers_Throws() {
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> driverMatchingService.selectDriverIdForRide(request, now))
                .isInstanceOf(NoActiveDriversException.class)
                .hasMessageContaining("no active drivers");
    }

    @Test(description = "Should return null when online drivers exist but none eligible - vehicle type mismatch")
    public void selectDriverIdForRide_VehicleTypeMismatch_ReturnsNull() {
        Driver sedanDriver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(sedanDriver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        request.setVehicleType("SUV");
        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should return null when baby transport required and driver not baby friendly")
    public void selectDriverIdForRide_NeedsBaby_DriverNotBabyFriendly_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        request.setBabyTransport(true);
        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should return null when pet transport required and driver not pet friendly")
    public void selectDriverIdForRide_NeedsPet_DriverNotPetFriendly_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        request.setPetTransport(true);
        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should return driver id when one free driver")
    public void selectDriverIdForRide_OneFreeDriver_ReturnsDriverId() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isEqualTo(1L);
    }

    @Test(description = "Should return nearest free driver when multiple free")
    public void selectDriverIdForRide_MultipleFree_ReturnsNearestToPickup() {
        Driver driver1 = driver(1L, "SEDAN", false, false);
        Driver driver2 = driver(2L, "SEDAN", false, false);
        DriverState state1 = driverState(driver1, false, 45.20, 19.80, null, null, null);
        DriverState state2 = driverState(driver2, false, 45.255, 19.845, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state1, state2));
        when(driverWorkLogRepository.findCompletedSince(any(), any())).thenReturn(Collections.emptyList());

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isEqualTo(2L);
    }

    @Test(description = "Should return null when driver has nextScheduledRideAt set")
    public void selectDriverIdForRide_NextScheduledRideSet_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        state.setNextScheduledRideAt(now.plusHours(1));
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should return null when driver exceeded 8h work limit")
    public void selectDriverIdForRide_ExceededWorkLimit_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        LocalDateTime since = now.minusHours(24);
        DriverWorkLog longLog = DriverWorkLog.builder()
                .driver(driver)
                .startedAt(since)
                .endedAt(since.plusHours(9))
                .completed(true)
                .createdAt(since)
                .build();
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(List.of(longLog));

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should return busy driver when current ride ends within 10 min")
    public void selectDriverIdForRide_BusyDriver_EndsWithin10Min_ReturnsDriverId() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, true, 45.24, 19.83,
                now.plusMinutes(5), 45.26, 19.85);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isEqualTo(1L);
    }

    @Test(description = "Should return null when all busy drivers end after 10 min")
    public void selectDriverIdForRide_AllBusy_EndAfter10Min_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, true, 45.24, 19.83,
                now.plusMinutes(15), null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "Should accept null vehicle type - all types eligible")
    public void selectDriverIdForRide_NullVehicleType_AcceptsDriver() {
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));
        when(driverWorkLogRepository.findCompletedSince(eq(1L), any())).thenReturn(Collections.emptyList());

        request.setVehicleType(null);
        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isEqualTo(1L);
    }

    @Test(description = "Should exclude blocked driver")
    public void selectDriverIdForRide_BlockedDriver_ReturnsNull() {
        Driver driver = driver(1L, "SEDAN", false, false);
        driver.setBlocked(true);
        DriverState state = driverState(driver, false, 45.24, 19.83, null, null, null);
        when(driverStateRepository.findAllOnlineDrivers()).thenReturn(List.of(state));

        Long result = driverMatchingService.selectDriverIdForRide(request, now);

        assertThat(result).isNull();
    }

    @Test(description = "hasExceededWorkLimit returns false when work under 8h")
    public void hasExceededWorkLimit_Under8h_ReturnsFalse() {
        LocalDateTime since = now.minusHours(24);
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .startedAt(since)
                .endedAt(since.plusHours(4))
                .completed(true)
                .createdAt(since)
                .build();
        when(driverWorkLogRepository.findCompletedSince(1L, since)).thenReturn(List.of(log));

        boolean result = driverMatchingService.hasExceededWorkLimit(1L, now);

        assertThat(result).isFalse();
    }

    @Test(description = "hasExceededWorkLimit returns true when work over 8h")
    public void hasExceededWorkLimit_Over8h_ReturnsTrue() {
        LocalDateTime since = now.minusHours(24);
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .startedAt(since)
                .endedAt(since.plusHours(9))
                .completed(true)
                .createdAt(since)
                .build();
        when(driverWorkLogRepository.findCompletedSince(1L, since)).thenReturn(List.of(log));

        boolean result = driverMatchingService.hasExceededWorkLimit(1L, now);

        assertThat(result).isTrue();
    }

    @Test(description = "hasExceededWorkLimit returns false when exactly 8h")
    public void hasExceededWorkLimit_Exactly8h_ReturnsFalse() {
        LocalDateTime since = now.minusHours(24);
        Driver driver = driver(1L, "SEDAN", false, false);
        DriverWorkLog log = DriverWorkLog.builder()
                .driver(driver)
                .startedAt(since)
                .endedAt(since.plusHours(8))
                .completed(true)
                .createdAt(since)
                .build();
        when(driverWorkLogRepository.findCompletedSince(1L, since)).thenReturn(List.of(log));

        boolean result = driverMatchingService.hasExceededWorkLimit(1L, now);

        assertThat(result).isFalse();
    }

    private static Driver driver(Long id, String vehicleType, boolean babyFriendly, boolean petFriendly) {
        Driver d = new Driver();
        d.setId(id);
        d.setVehicleType(vehicleType);
        d.setBabyFriendly(babyFriendly);
        d.setPetFriendly(petFriendly);
        d.setBlocked(false);
        return d;
    }

    private static DriverState driverState(Driver driver, boolean busy,
                                          Double lat, Double lon,
                                          LocalDateTime currentRideEndsAt,
                                          Double endLat, Double endLon) {
        DriverState ds = new DriverState();
        ds.setId(driver.getId());
        ds.setDriver(driver);
        ds.setOnline(true);
        ds.setBusy(busy);
        ds.setLatitude(lat);
        ds.setLongitude(lon);
        ds.setUpdatedAt(LocalDateTime.now());
        ds.setCurrentRideEndsAt(currentRideEndsAt);
        ds.setCurrentRideEndLatitude(endLat);
        ds.setCurrentRideEndLongitude(endLon);
        ds.setNextScheduledRideAt(null);
        return ds;
    }
}
