package com.pekara.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.pekara.dto.response.UserNotificationDto;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideNotificationServiceImpl implements RideNotificationService {

    private final MailService mailService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @Value("${firebase.projectId:}")
    private String firebaseProjectId;

    @Value("${firebase.databaseUrl:}")
    private String firebaseDatabaseUrl;

    @Value("${firebase.serviceAccountPath:}")
    private String firebaseServiceAccountPath;

    // --- Firebase/FCM configuration ---
    private static final String ADMINS_TOPIC = "admins";

    private volatile boolean firebaseInitialized = false;

    @PostConstruct
    public void init() {
        initFirebaseIfPossible();
    }

    @Override
    public void sendRideOrderNotifications(String driverEmail, String creatorEmail, Long rideId, String rideStatus, LocalDateTime scheduledAt, List<String> passengerEmails) {
        try {
            mailService.sendRideAssignedToDriver(driverEmail, rideId, scheduledAt);
        } catch (Exception e) {
            log.warn("Failed to send ride assignment email to driver {}: {}", driverEmail, e.getMessage());
        }

        // FCM to driver about new assignment
        try {
            sendFcmNotificationToUser(driverEmail,
                    "Ride assigned",
                    scheduledAt != null ? "You have a scheduled ride assigned" : "You have a new ride request",
                    Map.of(
                            "rideId", String.valueOf(rideId),
                            "status", Objects.toString(rideStatus, ""),
                            "type", "ASSIGNED"
                    ));
        } catch (Exception ex) {
            log.warn("Failed to send FCM to driver {}: {}", driverEmail, ex.getMessage());
        }

        try {
            mailService.sendRideOrderAccepted(creatorEmail, rideId, rideStatus);
        } catch (Exception e) {
            log.warn("Failed to send ride accepted email to creator {}: {}", creatorEmail, e.getMessage());
        }

        // FCM to creator that order was accepted (accepting is automatic per current logic)
        try {
            sendFcmNotificationToUser(creatorEmail,
                    "Ride accepted",
                    "Your ride request has been accepted",
                    Map.of(
                            "rideId", String.valueOf(rideId),
                            "status", Objects.toString(rideStatus, ""),
                            "type", "ACCEPTED"
                    ));
        } catch (Exception ex) {
            log.warn("Failed to send FCM to creator {}: {}", creatorEmail, ex.getMessage());
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

                try {
                    UserNotificationDto notification = UserNotificationDto.builder()
                            .rideId(rideId)
                            .status(rideStatus)
                            .message("You have been added to a ride and it has been accepted.")
                            .build();
                    messagingTemplate.convertAndSend("/topic/notifications/" + email, notification);
                } catch (Exception e) {
                    log.warn("Failed to send WebSocket notification to passenger {}: {}", email, e.getMessage());
                }

                // FCM to passenger about being added to accepted ride
                try {
                    sendFcmNotificationToUser(email,
                            "Ride invitation",
                            "You have been added to a ride",
                            Map.of(
                                    "rideId", String.valueOf(rideId),
                                    "status", Objects.toString(rideStatus, ""),
                                    "type", "INVITED"
                            ));
                } catch (Exception ex) {
                    log.warn("Failed to send FCM to passenger {}: {}", email, ex.getMessage());
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
        try {
            sendFcmNotificationToUser(email,
                    "Ride rejected",
                    reason != null ? reason : "Your ride request was rejected",
                    Map.of("type", "REJECTED"));
        } catch (Exception ex) {
            log.warn("Failed to send FCM rejection to {}: {}", email, ex.getMessage());
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
        try {
            if (driverEmail != null && !isDriver) {
                sendFcmNotificationToUser(driverEmail,
                        "Ride cancelled",
                        "Ride was cancelled by " + (isCreator ? "passenger" : "user"),
                        Map.of("type", "CANCELLED"));
            }
            if (!isCreator && creatorEmail != null) {
                sendFcmNotificationToUser(creatorEmail,
                        "Ride cancelled",
                        (isDriver ? "Driver cancelled the ride" : "Ride has been cancelled"),
                        Map.of("type", "CANCELLED"));
            }
        } catch (Exception ex) {
            log.warn("Failed to send FCM cancellation notification: {}", ex.getMessage());
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
                log.info("Sent ride completion email to {}", email);
            } catch (Exception e) {
                log.warn("Failed to send ride completion email to {}: {}", email, e.getMessage());
            }

            try {
                UserNotificationDto notification = UserNotificationDto.builder()
                        .rideId(rideId)
                        .status("COMPLETED")
                        .message("Your ride has been successfully completed.")
                        .build();
                messagingTemplate.convertAndSend("/topic/notifications/" + email, notification);
            } catch (Exception e) {
                log.warn("Failed to send WebSocket completion notification to {}: {}", email, e.getMessage());
            }

            // FCM completion notification
            try {
                sendFcmNotificationToUser(email,
                        "Ride completed",
                        "Your ride has been completed",
                        Map.of(
                                "rideId", String.valueOf(rideId),
                                "type", "COMPLETED",
                                "price", finalPrice != null ? finalPrice.toPlainString() : ""
                        ));
            } catch (Exception ex) {
                log.warn("Failed to send FCM completion notification to {}: {}", email, ex.getMessage());
            }
        }
    }

    // ----------------- FCM helpers -----------------
    private void initFirebaseIfPossible() {
        if (firebaseInitialized) return;
        synchronized (RideNotificationServiceImpl.class) {
            if (firebaseInitialized) return;
            try {
                if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
                    log.warn("Firebase Project ID not set, skipping initialization");
                    return;
                }
                GoogleCredentials credentials;
                if (firebaseServiceAccountPath != null && !firebaseServiceAccountPath.isBlank()) {
                    try {
                        credentials = GoogleCredentials.fromStream(new FileInputStream(firebaseServiceAccountPath));
                        log.info("Firebase loaded credentials from specified path: {}", firebaseServiceAccountPath);
                    } catch (IOException ex) {
                        log.warn("Failed to load Firebase credentials from {}, attempting application default: {}", 
                                firebaseServiceAccountPath, ex.getMessage());
                        credentials = GoogleCredentials.getApplicationDefault();
                    }
                } else {
                    credentials = GoogleCredentials.getApplicationDefault();
                }

                FirebaseOptions.Builder builder = FirebaseOptions.builder().setCredentials(credentials);
                if (firebaseDatabaseUrl != null && !firebaseDatabaseUrl.isBlank()) {
                    builder.setDatabaseUrl(firebaseDatabaseUrl);
                }
                FirebaseOptions options = builder.build();
                if (FirebaseApp.getApps().isEmpty()) {
                    FirebaseApp.initializeApp(options);
                }
                firebaseInitialized = true;
                log.info("FirebaseApp initialized with project ID: {}{}",
                        firebaseProjectId,
                        firebaseDatabaseUrl != null ? " and DB URL" : "");
            } catch (Exception e) {
                // Do not fail the app if Firebase is not configured in local/dev
                log.warn("Firebase initialization skipped: {}", e.getMessage());
                firebaseInitialized = false;
            }
        }
    }

    private void sendFcmNotificationToUser(String email, String title, String body, Map<String, String> data) throws IOException {
        if (email == null || email.isBlank()) return;
        try {
            initFirebaseIfPossible();
            if (!firebaseInitialized) {
                log.info("Firebase not initialized; skipping send");
                return;
            }

            // Prefer sending to a specific device token if available
            String token = null;
            try {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    token = user.getDeviceToken();
                }
            } catch (Exception ex) {
                log.warn("Unable to fetch user/device token for {}: {}", email, ex.getMessage());
            }

            Message.Builder builder = Message.builder();
            if (token != null && !token.isBlank()) {
                builder.setToken(token);
            } else {
                log.info("No device token for {}; cannot send FCM (topics disabled)", email);
                return;
            }

            // Notification section
            if (title != null || body != null) {
                Notification.Builder n = Notification.builder();
                if (title != null) n.setTitle(title);
                if (body != null) n.setBody(body);
                builder.setNotification(n.build());
            }

            if (data != null && !data.isEmpty()) {
                builder.putAllData(data);
            }

            FirebaseMessaging.getInstance().send(builder.build());
            log.info("FCM sent to {} using token", email);
        } catch (Exception e) {
            log.warn("Failed to send FCM to {}: {}", email, e.getMessage());
        }
    }


    @Override
    public void registerClientToken(String email, String fcmToken) {
        log.info("[DEBUG_LOG] registerClientToken called for email: {}, token starts with: {}", 
                email, (fcmToken != null && fcmToken.length() > 10 ? fcmToken.substring(0, 10) : fcmToken));
        
        if (email == null || email.isBlank() || fcmToken == null || fcmToken.isBlank()) {
            log.warn("[DEBUG_LOG] Cannot register FCM token: email or token is blank. Email: {}, Token present: {}", 
                    email, fcmToken != null);
            return;
        }
        try {
            // Persist token to user profile
            userRepository.findByEmail(email).ifPresentOrElse(user -> {
                try {
                    user.setDeviceToken(fcmToken);
                    userRepository.save(user);
                    log.info("[DEBUG_LOG] Successfully saved device token for user {}", email);
                } catch (Exception ex) {
                    log.error("[DEBUG_LOG] Failed to save device token for {}: {}", email, ex.getMessage(), ex);
                }
            }, () -> {
                log.warn("[DEBUG_LOG] User {} not found in database, cannot save FCM token", email);
            });
        } catch (Exception e) {
            log.error("[DEBUG_LOG] Unexpected error during FCM token registration for {}: {}", email, e.getMessage(), e);
        }
    }

    @Override
    public void unsubscribeFromAdminTopic(String fcmToken) {
        // No-op: topics disabled
    }

    @Override
    public void sendPanicNotificationToAdmins(Long rideId, String panickedBy, String activatorEmail) {
        try {
            initFirebaseIfPossible();
            if (!firebaseInitialized) {
                log.debug("Firebase not initialized; skipping panic notification");
                return;
            }

            String title = "🚨 PANIC BUTTON ACTIVATED";
            String body = String.format("Ride #%d - Panic activated by %s (%s)", rideId, panickedBy, activatorEmail);

            // Find all admins and send to their tokens
            List<User> admins = userRepository.findByRole(UserRole.ADMIN);
            for (User admin : admins) {
                String token = admin.getDeviceToken();
                if (token != null && !token.isBlank()) {
                    try {
                        Message message = Message.builder()
                                .setToken(token)
                                .setNotification(Notification.builder()
                                        .setTitle(title)
                                        .setBody(body)
                                        .build())
                                .putData("rideId", String.valueOf(rideId))
                                .putData("panickedBy", panickedBy)
                                .putData("activatorEmail", activatorEmail)
                                .putData("type", "PANIC")
                                .build();

                        FirebaseMessaging.getInstance().send(message);
                    } catch (Exception e) {
                        log.warn("Failed to send panic FCM to admin {}: {}", admin.getEmail(), e.getMessage());
                    }
                }
            }
            log.info("Panic notifications sent to available admin tokens for ride {}", rideId);
        } catch (Exception e) {
            log.error("Failed to send panic notifications to admins: {}", e.getMessage(), e);
        }
    }

    private static String toUserTopic(String email) {
        // Topic names must match [a-zA-Z0-9-_.~%]
        String sanitized = email.trim().toLowerCase()
                .replace('@', '-')
                .replaceAll("[^a-z0-9-_.~%]", "-");
        return "user-" + sanitized;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

}
