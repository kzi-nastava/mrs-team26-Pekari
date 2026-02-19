package com.pekara.service;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.pekara.dto.response.UserNotificationDto;
import com.pekara.model.User;
import com.pekara.model.UserRole;
import com.pekara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    // --- Firebase/FCM configuration ---
    private static final String FCM_BASE_URL = "https://fcm.googleapis.com";
    // Use HTTP v1 endpoint: /v1/projects/{projectId}/messages:send
    private static final String FCM_SEND_ENDPOINT_TEMPLATE = "/v1/projects/%s/messages:send";
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
                GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
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
        String topic = toUserTopic(email);
        String endpoint = buildFcmSendEndpoint();
        if (endpoint == null) {
            log.debug("FCM endpoint not configured, skipping");
            return;
        }
        HttpURLConnection conn = createFcmConnection(endpoint);

        StringBuilder json = new StringBuilder();
        json.append('{')
                .append("\"message\":{")
                .append("\"topic\":\"").append(topic).append("\",")
                .append("\"notification\":{")
                .append("\"title\":\"").append(escapeJson(title)).append("\",")
                .append("\"body\":\"").append(escapeJson(body)).append("\"},");
        if (data != null && !data.isEmpty()) {
            json.append("\"data\":{");
            boolean first = true;
            for (Map.Entry<String, String> e : data.entrySet()) {
                if (!first) json.append(',');
                first = false;
                json.append("\"").append(escapeJson(e.getKey())).append("\":\"")
                        .append(escapeJson(Objects.toString(e.getValue(), ""))).append("\"");
            }
            json.append('}');
        } else {
            // remove trailing comma added after notification
            if (json.charAt(json.length() - 1) == ',') {
                json.setLength(json.length() - 1);
            }
        }
        json.append('}').append('}');

        byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
        conn.setDoOutput(true);
        conn.getOutputStream().write(bytes);

        int code = conn.getResponseCode();
        if (code / 100 != 2) {
            log.warn("FCM send failed for {} with HTTP {}", email, code);
        } else {
            log.debug("FCM sent to {} on topic {}", email, topic);
        }
        conn.disconnect();
    }

    private HttpURLConnection createFcmConnection(String endpoint) throws IOException {
        URL url = new URL(FCM_BASE_URL + endpoint);
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setRequestProperty("Authorization", "Bearer " + getServiceAccountAccessToken());
        httpURLConnection.setRequestProperty("Content-Type", "application/json; UTF-8");
        return httpURLConnection;
    }

    private String getServiceAccountAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
                .createScoped("https://www.googleapis.com/auth/firebase.messaging");
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        if (token == null) {
            credentials.refresh();
            token = credentials.getAccessToken();
        }
        return token != null ? token.getTokenValue() : null;
    }

    private String buildFcmSendEndpoint() {
        if (firebaseProjectId == null || firebaseProjectId.isBlank()) {
            log.warn("Firebase Project ID not set; cannot send FCM");
            return null;
        }
        return String.format(FCM_SEND_ENDPOINT_TEMPLATE, firebaseProjectId);
    }

    @Override
    public void registerClientToken(String email, String fcmToken) {
        if (email == null || email.isBlank() || fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        try {
            initFirebaseIfPossible();
            if (!firebaseInitialized) {
                log.debug("Firebase not initialized; skipping token registration");
                return;
            }

            // Subscribe to user's personal topic
            String topic = toUserTopic(email);
            FirebaseMessaging.getInstance().subscribeToTopic(java.util.List.of(fcmToken), topic);
            log.info("Subscribed client token to topic {} for user {}", topic, email);

            // If user is an admin, also subscribe to admins topic
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.getRole() == UserRole.ADMIN) {
                FirebaseMessaging.getInstance().subscribeToTopic(java.util.List.of(fcmToken), ADMINS_TOPIC);
                log.info("Subscribed admin {} to admins topic", email);
            }
        } catch (Exception e) {
            log.warn("Failed to subscribe token for {}: {}", email, e.getMessage());
        }
    }

    @Override
    public void unsubscribeFromAdminTopic(String fcmToken) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }
        try {
            initFirebaseIfPossible();
            if (!firebaseInitialized) {
                log.debug("Firebase not initialized; skipping token unsubscription");
                return;
            }
            FirebaseMessaging.getInstance().unsubscribeFromTopic(java.util.List.of(fcmToken), ADMINS_TOPIC);
            log.info("Unsubscribed token from admins topic");
        } catch (Exception e) {
            log.warn("Failed to unsubscribe token from admins topic: {}", e.getMessage());
        }
    }

    @Override
    public void sendPanicNotificationToAdmins(Long rideId, String panickedBy, String activatorEmail) {
        try {
            String endpoint = buildFcmSendEndpoint();
            if (endpoint == null) {
                log.debug("FCM endpoint not configured, skipping panic notification");
                return;
            }

            HttpURLConnection conn = createFcmConnection(endpoint);

            String title = "🚨 PANIC BUTTON ACTIVATED";
            String body = String.format("Ride #%d - Panic activated by %s (%s)", rideId, panickedBy, activatorEmail);

            StringBuilder json = new StringBuilder();
            json.append('{')
                    .append("\"message\":{")
                    .append("\"topic\":\"").append(ADMINS_TOPIC).append("\",")
                    .append("\"notification\":{")
                    .append("\"title\":\"").append(escapeJson(title)).append("\",")
                    .append("\"body\":\"").append(escapeJson(body)).append("\"},")
                    .append("\"data\":{")
                    .append("\"rideId\":\"").append(rideId).append("\",")
                    .append("\"panickedBy\":\"").append(escapeJson(panickedBy)).append("\",")
                    .append("\"activatorEmail\":\"").append(escapeJson(activatorEmail)).append("\",")
                    .append("\"type\":\"PANIC\"")
                    .append("}}}");

            byte[] bytes = json.toString().getBytes(StandardCharsets.UTF_8);
            conn.setDoOutput(true);
            conn.getOutputStream().write(bytes);

            int code = conn.getResponseCode();
            if (code / 100 != 2) {
                log.error("Failed to send panic notification to admins with HTTP {}", code);
            } else {
                log.warn("Panic notification sent to admins for ride {}", rideId);
            }
            conn.disconnect();
        } catch (Exception e) {
            log.error("Failed to send panic notification to admins: {}", e.getMessage(), e);
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

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
