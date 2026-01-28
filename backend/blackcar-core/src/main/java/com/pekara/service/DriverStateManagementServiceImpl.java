package com.pekara.service;

import com.pekara.model.DriverState;
import com.pekara.repository.DriverStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class DriverStateManagementServiceImpl implements DriverStateManagementService {

    private final DriverStateRepository driverStateRepository;

    @Override
    @Transactional
    public void releaseDriverAfterRide(Long driverId) {
        if (driverId == null) {
            return;
        }

        DriverState driverState = driverStateRepository.findById(driverId).orElse(null);
        if (driverState != null) {
            driverState.setBusy(false);
            driverState.setCurrentRideEndsAt(null);
            driverState.setCurrentRideEndLatitude(null);
            driverState.setCurrentRideEndLongitude(null);
            driverStateRepository.save(driverState);
            log.debug("Driver {} released from ride", driverId);
        }
    }

    @Override
    @Transactional
    public void releaseDriverAndClearSchedule(Long driverId) {
        if (driverId == null) {
            return;
        }

        DriverState driverState = driverStateRepository.findById(driverId).orElse(null);
        if (driverState != null) {
            driverState.setBusy(false);
            driverState.setCurrentRideEndsAt(null);
            driverState.setCurrentRideEndLatitude(null);
            driverState.setCurrentRideEndLongitude(null);
            driverState.setNextScheduledRideAt(null);
            driverStateRepository.save(driverState);
            log.debug("Driver {} released and schedule cleared", driverId);
        }
    }

    @Override
    @Transactional
    public void markDriverBusy(Long driverId, int estimatedDurationMinutes, double endLatitude, double endLongitude) {
        DriverState driverState = driverStateRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver state not found"));

        LocalDateTime now = LocalDateTime.now();
        driverState.setBusy(true);
        driverState.setCurrentRideEndsAt(now.plusMinutes(estimatedDurationMinutes));
        driverState.setCurrentRideEndLatitude(endLatitude);
        driverState.setCurrentRideEndLongitude(endLongitude);
        driverStateRepository.save(driverState);
    }

    @Override
    @Transactional
    public void setNextScheduledRide(Long driverId, LocalDateTime scheduledAt) {
        DriverState driverState = driverStateRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver state not found"));

        driverState.setNextScheduledRideAt(scheduledAt);
        driverStateRepository.save(driverState);
    }
}
