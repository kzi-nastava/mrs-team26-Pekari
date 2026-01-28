package com.pekara.service;

import com.pekara.model.DriverWorkLog;
import com.pekara.model.Ride;
import com.pekara.repository.DriverWorkLogRepository;
import com.pekara.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideWorkLogServiceImpl implements RideWorkLogService {

    private final DriverWorkLogRepository driverWorkLogRepository;
    private final RideRepository rideRepository;

    @Override
    @Transactional
    public void createWorkLogForRide(Long rideId, Long driverId, LocalDateTime startedAt) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        driverWorkLogRepository.save(DriverWorkLog.builder()
                .driver(ride.getDriver())
                .ride(ride)
                .startedAt(startedAt)
                .endedAt(null)
                .completed(false)
                .build());
    }

    @Override
    @Transactional
    public void startWorkLog(Long rideId, LocalDateTime startedAt) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        DriverWorkLog workLog = findOrCreateWorkLog(ride, startedAt);
        workLog.setStartedAt(startedAt);
        driverWorkLogRepository.save(workLog);
    }

    @Override
    @Transactional
    public void completeWorkLog(Long rideId, LocalDateTime endTime) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        DriverWorkLog workLog = findOrCreateWorkLog(ride, endTime);
        workLog.setEndedAt(endTime);
        workLog.setCompleted(true);
        driverWorkLogRepository.save(workLog);
        log.debug("Work log completed for ride {}: {} to {}",
                ride.getId(), workLog.getStartedAt(), workLog.getEndedAt());
    }

    @Override
    @Transactional
    public void cancelWorkLog(Long rideId, LocalDateTime endTime) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new IllegalArgumentException("Ride not found"));

        driverWorkLogRepository.findByRide(ride).ifPresent(workLog -> {
            workLog.setCompleted(false);
            workLog.setEndedAt(endTime);
            driverWorkLogRepository.save(workLog);
            log.debug("Work log cancelled for ride {}", ride.getId());
        });
    }

    private DriverWorkLog findOrCreateWorkLog(Ride ride, LocalDateTime defaultStartTime) {
        return driverWorkLogRepository.findByRide(ride)
                .orElseGet(() -> DriverWorkLog.builder()
                        .driver(ride.getDriver())
                        .ride(ride)
                        .startedAt(ride.getStartedAt() != null ? ride.getStartedAt() : defaultStartTime)
                        .build());
    }
}
