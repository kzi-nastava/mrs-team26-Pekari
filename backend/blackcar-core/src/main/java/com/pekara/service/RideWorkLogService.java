package com.pekara.service;

import java.time.LocalDateTime;

public interface RideWorkLogService {

    void createWorkLogForRide(Long rideId, Long driverId, LocalDateTime startedAt);

    void startWorkLog(Long rideId, LocalDateTime startedAt);

    void completeWorkLog(Long rideId, LocalDateTime endTime);

    void cancelWorkLog(Long rideId, LocalDateTime endTime);
}
