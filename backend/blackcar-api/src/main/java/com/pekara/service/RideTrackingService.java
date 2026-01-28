package com.pekara.service;

import com.pekara.dto.request.RideLocationUpdateRequest;
import com.pekara.dto.response.RideTrackingResponse;

public interface RideTrackingService {

    /**
     * Persist the driver's latest location for the given ride in fast storage.
     */
    void updateLocation(Long rideId, String driverEmail, RideLocationUpdateRequest request);

    /**
     * Fetch the latest tracking snapshot for a ride for an authorized participant.
     */
    RideTrackingResponse getTracking(Long rideId, String requesterEmail);
}
