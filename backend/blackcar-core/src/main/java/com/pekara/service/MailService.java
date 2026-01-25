package com.pekara.service;

public interface MailService {

    void sendActivationEmail(String toEmail, String activationToken);
  
    void sendDriverActivationEmail(String toEmail, String activationToken, String driverName);
  
    void sendRideAssignedToDriver(String driverEmail, Long rideId, java.time.LocalDateTime scheduledAt);

    void sendRideOrderAccepted(String toEmail, Long rideId, String status);

    void sendRideOrderRejected(String toEmail, String reason);

    void sendRideDetailsShared(String toEmail, Long rideId, String creatorEmail);

    void sendRideReminder(String toEmail, Long rideId, java.time.LocalDateTime scheduledAt);
}
