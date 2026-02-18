package com.example.blackcar.data.api.model;

import java.math.BigDecimal;

/**
 * Response from POST /rides/order
 * status: ACCEPTED, REJECTED, SCHEDULED
 */
public class OrderRideResponse {

    private Long rideId;
    private String status;
    private String message;
    private BigDecimal estimatedPrice;
    private String scheduledAt;
    private String assignedDriverEmail;

    public OrderRideResponse() {}

    public Long getRideId() { return rideId; }
    public void setRideId(Long rideId) { this.rideId = rideId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BigDecimal getEstimatedPrice() { return estimatedPrice; }
    public void setEstimatedPrice(BigDecimal estimatedPrice) { this.estimatedPrice = estimatedPrice; }

    public String getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(String scheduledAt) { this.scheduledAt = scheduledAt; }

    public String getAssignedDriverEmail() { return assignedDriverEmail; }
    public void setAssignedDriverEmail(String assignedDriverEmail) {
        this.assignedDriverEmail = assignedDriverEmail;
    }
}
