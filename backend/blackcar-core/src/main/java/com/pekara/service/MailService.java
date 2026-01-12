package com.pekara.service;

public interface MailService {

    /**
     * Send account activation email to user.
     *
     * @param toEmail recipient email address
     * @param activationToken activation token to include in email
     */
    void sendActivationEmail(String toEmail, String activationToken);

    void sendRideAssignedToDriver(String driverEmail, Long rideId, java.time.LocalDateTime scheduledAt);

    void sendRideOrderAccepted(String toEmail, Long rideId, String status);

    void sendRideOrderRejected(String toEmail, String reason);

    void sendRideDetailsShared(String toEmail, Long rideId, String creatorEmail);

    void sendRideReminder(String toEmail, Long rideId, java.time.LocalDateTime scheduledAt);
}
