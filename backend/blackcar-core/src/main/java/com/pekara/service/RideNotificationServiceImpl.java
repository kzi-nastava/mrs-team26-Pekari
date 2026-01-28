package com.pekara.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideNotificationServiceImpl implements RideNotificationService {

    private final MailService mailService;

    @Override
    public void sendRideOrderNotifications(String driverEmail, String creatorEmail, Long rideId, String rideStatus, LocalDateTime scheduledAt, List<String> passengerEmails) {
        try {
            mailService.sendRideAssignedToDriver(driverEmail, rideId, scheduledAt);
        } catch (Exception e) {
            log.warn("Failed to send ride assignment email to driver {}: {}", driverEmail, e.getMessage());
        }

        try {
            mailService.sendRideOrderAccepted(creatorEmail, rideId, rideStatus);
        } catch (Exception e) {
            log.warn("Failed to send ride accepted email to creator {}: {}", creatorEmail, e.getMessage());
        }

        if (passengerEmails != null) {
            for (String email : passengerEmails) {
                if (email == null || email.isBlank()) {
                    continue;
                }
                try {
                    mailService.sendRideDetailsShared(email, rideId, creatorEmail);
                } catch (Exception e) {
                    log.warn("Failed to send ride details email to passenger {}: {}", email, e.getMessage());
                }
            }
        }
    }

    @Override
    public void sendRejectionNotification(String email, String reason) {
        try {
            mailService.sendRideOrderRejected(email, reason);
        } catch (Exception e) {
            log.warn("Failed to send rejection email to {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void sendCancellationNotifications(String driverEmail, String creatorEmail, boolean isDriver, boolean isCreator, String reason) {
        try {
            if (driverEmail != null && !isDriver) {
                mailService.sendRideOrderRejected(driverEmail,
                        "Ride cancelled by " + (isCreator ? "passenger" : "user") + ": " + reason);
            }
            if (!isCreator && creatorEmail != null) {
                mailService.sendRideOrderRejected(creatorEmail,
                        "Ride cancelled" + (isDriver ? " by driver" : "") + ": " + reason);
            }
        } catch (Exception e) {
            log.warn("Failed to send cancellation notification: {}", e.getMessage());
        }
    }

    @Override
    public void sendRideCompletionNotifications(Long rideId, List<String> passengerEmails, java.math.BigDecimal finalPrice) {
        if (passengerEmails == null || passengerEmails.isEmpty()) {
            return;
        }

        for (String email : passengerEmails) {
            if (email == null || email.isBlank()) {
                continue;
            }
            try {
                mailService.sendRideCompleted(email, rideId, finalPrice);
                log.info("Sent ride completion notification to {}", email);
            } catch (Exception e) {
                log.warn("Failed to send ride completion email to {}: {}", email, e.getMessage());
            }
        }
    }
}
