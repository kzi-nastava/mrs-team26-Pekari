package com.pekara.service;

public interface DriverStateManagementService {

    void releaseDriverAfterRide(Long driverId);

    void releaseDriverAndClearSchedule(Long driverId);

    void markDriverBusy(Long driverId, int estimatedDurationMinutes, double endLatitude, double endLongitude);

    void setNextScheduledRide(Long driverId, java.time.LocalDateTime scheduledAt);
}
