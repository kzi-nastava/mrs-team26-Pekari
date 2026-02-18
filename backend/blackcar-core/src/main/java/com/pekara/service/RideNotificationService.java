package com.pekara.service;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

public interface RideNotificationService {

    void sendRideOrderNotifications(String driverEmail, String creatorEmail, Long rideId, String rideStatus, LocalDateTime scheduledAt, List<String> passengerEmails);

    void sendRejectionNotification(String email, String reason);

    void sendCancellationNotifications(String driverEmail, String creatorEmail, boolean isDriver, boolean isCreator, String reason);
    void sendRideCompletionNotifications(Long rideId, List<String> passengerEmails, BigDecimal finalPrice);

    /**
     * Register a client FCM token for the given user email and subscribe it to that user's topic.
     * No-ops gracefully if Firebase isn't configured.
     */
    void registerClientToken(String email, String fcmToken);
}
