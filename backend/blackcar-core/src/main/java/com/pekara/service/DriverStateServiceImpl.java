package com.pekara.service;

import com.pekara.dto.request.UpdateDriverLocationRequest;
import com.pekara.dto.request.UpdateDriverOnlineStatusRequest;
import com.pekara.dto.response.DriverStateResponse;
import com.pekara.dto.response.OnlineDriverWithVehicleResponse;
import com.pekara.model.Driver;
import com.pekara.model.DriverState;
import com.pekara.repository.DriverRepository;
import com.pekara.repository.DriverStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DriverStateServiceImpl implements com.pekara.service.DriverStateService {

    private final DriverRepository driverRepository;
    private final DriverStateRepository driverStateRepository;

    @Override
    @Transactional
    public DriverStateResponse updateOnlineStatus(String driverEmail, UpdateDriverOnlineStatusRequest request) {
        Driver driver = driverRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        DriverState state = driverStateRepository.findById(driver.getId())
                .orElseGet(() -> DriverState.builder().driver(driver).online(false).busy(false).build());

        state.setOnline(Boolean.TRUE.equals(request.getOnline()));
        state.setUpdatedAt(LocalDateTime.now());

        DriverState saved = driverStateRepository.save(state);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DriverStateResponse updateLocation(String driverEmail, UpdateDriverLocationRequest request) {
        Driver driver = driverRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }

        DriverState state = driverStateRepository.findById(driver.getId())
                .orElseGet(() -> DriverState.builder().driver(driver).online(false).busy(false).build());

        state.setLatitude(request.getLatitude());
        state.setLongitude(request.getLongitude());
        state.setUpdatedAt(LocalDateTime.now());

        DriverState saved = driverStateRepository.save(state);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverStateResponse getMyState(String driverEmail) {
        Driver driver = driverRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found"));

        DriverState state = driverStateRepository.findById(driver.getId())
                .orElseGet(() -> DriverState.builder().driver(driver).online(false).busy(false).build());

        return toResponse(state);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverStateResponse> getOnlineDrivers(int page, int size) {
        return driverStateRepository.findOnlineDrivers(PageRequest.of(page, size))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OnlineDriverWithVehicleResponse> getOnlineDriversWithVehicles(int page, int size) {
        if (size <= 0) {
            return List.of();
        }

        List<DriverState> onlineStates = driverStateRepository.findAllOnlineDrivers();
        Map<Long, Driver> activeDrivers = driverRepository.findAllActiveDrivers()
                .stream()
                .collect(Collectors.toMap(Driver::getId, Function.identity()));

        long offset = (long) page * size;
        return onlineStates.stream()
                .map(state -> {
                    Long driverId = state.getDriver() != null ? state.getDriver().getId() : state.getId();
                    Driver driver = activeDrivers.get(driverId);
                    if (driver == null) {
                        return null;
                    }
                    return toOnlineResponse(state, driver);
                })
                .filter(Objects::nonNull)
                .skip(offset)
                .limit(size)
                .toList();
    }

    private DriverStateResponse toResponse(DriverState state) {
        return toResponse(state, null);
    }

    private DriverStateResponse toResponse(DriverState state, Driver driver) {
        Long driverId = driver != null
                ? driver.getId()
                : (state.getDriver() != null ? state.getDriver().getId() : state.getId());
        String driverEmail = driver != null
                ? driver.getEmail()
                : (state.getDriver() != null ? state.getDriver().getEmail() : null);
        return DriverStateResponse.builder()
                .driverId(driverId)
                .driverEmail(driverEmail)
                .online(state.getOnline())
                .busy(state.getBusy())
                .latitude(state.getLatitude())
                .longitude(state.getLongitude())
                .updatedAt(state.getUpdatedAt())
                .build();
    }

    private OnlineDriverWithVehicleResponse toOnlineResponse(DriverState state, Driver driver) {
        return OnlineDriverWithVehicleResponse.builder()
                .driverState(toResponse(state, driver))
                .vehicleRegistration(driver.getLicensePlate())
                .vehicleType(driver.getVehicleType())
                .build();
    }
}
