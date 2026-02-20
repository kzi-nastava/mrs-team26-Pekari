package com.example.blackcar.data.api.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.blackcar.R;
import com.example.blackcar.data.auth.TokenManager;
import com.example.blackcar.data.repository.NotificationRepository;
import com.example.blackcar.presentation.home.MainActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * Handles panic alerts for admins and other notifications.
 */
public class PushNotificationService extends FirebaseMessagingService {

    private static final String TAG = "PushNotificationService";
    private static final String CHANNEL_ID_PANIC = "panic_alerts";
    private static final String CHANNEL_NAME_PANIC = "Panic Alerts";
    private static final String CHANNEL_ID_DEFAULT = "default_notifications";
    private static final String CHANNEL_NAME_DEFAULT = "Ride Notifications";
    private static final int NOTIFICATION_ID_PANIC = 1001;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.i(TAG, "[DEBUG_LOG] FCM token refreshed: " + token);

        // Ensure ApiClient is initialized for repository usage
        com.example.blackcar.data.api.ApiClient.init(getApplicationContext());

        // Save token locally
        TokenManager tokenManager = TokenManager.getInstance(getApplicationContext());
        tokenManager.saveFcmToken(token);

        // If user is logged in (persisted auth), register the new token with backend
        if (tokenManager.hasToken()) {
            Log.i(TAG, "[DEBUG_LOG] Auth token present, calling registerToken for new FCM token: " + token.substring(0, Math.min(token.length(), 10)) + "...");
            NotificationRepository repository = new NotificationRepository();
            repository.registerToken(token, new NotificationRepository.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "[DEBUG_LOG] New FCM token registered with backend successfully");
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "[DEBUG_LOG] Failed to register new FCM token on refresh: " + error);
                }
            });
        } else {
            Log.i(TAG, "[DEBUG_LOG] Auth token not found in TokenManager; deferring FCM registration until login");
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.i(TAG, "[DEBUG_LOG] From: " + remoteMessage.getFrom());

        // Check if message contains a data payload
        Map<String, String> data = remoteMessage.getData();
        if (!data.isEmpty()) {
            Log.i(TAG, "[DEBUG_LOG] Message data payload: " + data);
            String type = data.get("type");

            if ("PANIC".equals(type)) {
                handlePanicNotification(remoteMessage, data);
            } else if ("ASSIGNED".equals(type) || "ACCEPTED".equals(type) || "INVITED".equals(type) || "COMPLETED".equals(type)) {
                handleRideEventNotification(remoteMessage, data);
            } else if ("REJECTED".equals(type) || "CANCELLED".equals(type)) {
                handleRideCancellationNotification(remoteMessage, data);
            } else {
                handleGenericNotification(remoteMessage);
            }
        }

        // Check if message contains a notification payload
        if (remoteMessage.getNotification() != null) {
            Log.i(TAG, "[DEBUG_LOG] Message Notification Body: " + remoteMessage.getNotification().getBody());
            // If we didn't handle it via data payload (which is preferred for our logic), 
            // and it's a standard notification, ensure it's shown if not already shown
            if (data.isEmpty()) {
                handleGenericNotification(remoteMessage);
            }
        }
    }

    /**
     * Handle panic button activation notification.
     * Shows high-priority notification and navigates to Panic Panel on tap.
     */
    private void handlePanicNotification(RemoteMessage remoteMessage, Map<String, String> data) {
        String rideId = data.get("rideId");
        String panickedBy = data.get("panickedBy");
        String activatorEmail = data.get("activatorEmail");

        Log.w(TAG, "PANIC NOTIFICATION - Ride: " + rideId + ", By: " + panickedBy);

        // Create intent to open Panic Panel
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigate_to", "panic_panel");
        intent.putExtra("ride_id", rideId);
        intent.putExtra("panicked_by", panickedBy);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get notification content
        String title;
        String body;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else {
            title = "PANIC BUTTON ACTIVATED";
            body = "Ride #" + rideId + " - " + panickedBy + " activated panic";
        }

        // Build and show notification
        createNotificationChannels();
        showPanicNotification(title, body, pendingIntent);
    }

    /**
     * Handle ride-related events: ASSIGNED, ACCEPTED, INVITED, COMPLETED.
     * Navigates to ride details when tapped.
     */
    private void handleRideEventNotification(RemoteMessage remoteMessage, Map<String, String> data) {
        String rideId = data != null ? data.get("rideId") : null;
        String type = data != null ? data.get("type") : null;

        String title;
        String body;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else {
            // Fallback copy if notification section isn't provided
            if ("ASSIGNED".equals(type)) {
                title = "Ride assigned";
                body = "You have a new ride" + (rideId != null ? " (#" + rideId + ")" : "");
            } else if ("ACCEPTED".equals(type)) {
                title = "Ride accepted";
                body = "Your ride was accepted" + (rideId != null ? " (#" + rideId + ")" : "");
            } else if ("COMPLETED".equals(type)) {
                title = "Ride completed";
                body = "Your ride has been completed" + (rideId != null ? " (#" + rideId + ")" : "");
            } else {
                title = "Ride invitation";
                body = "You have been added to a ride" + (rideId != null ? " (#" + rideId + ")" : "");
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("navigate_to", "ride_details");
        if (rideId != null) intent.putExtra("ride_id", rideId);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannels();
        showGenericNotification(title, body, pendingIntent);
    }

    private void handleRideCancellationNotification(RemoteMessage remoteMessage, Map<String, String> data) {
        String type = data != null ? data.get("type") : null;

        String title;
        String body;
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else {
            if ("REJECTED".equals(type)) {
                title = "Ride rejected";
                body = "Your ride request was rejected";
            } else {
                title = "Ride cancelled";
                body = "Your ride has been cancelled";
            }
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannels();
        showGenericNotification(title, body, pendingIntent);
    }

    /**
     * Handle generic notifications (non-panic).
     */
    private void handleGenericNotification(RemoteMessage remoteMessage) {
        if (remoteMessage.getNotification() == null) {
            return;
        }

        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        createNotificationChannels();
        showGenericNotification(title, body, pendingIntent);
    }

    /**
     * Create notification channels for panic alerts and general notifications (Android 8+).
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // Panic Channel
            NotificationChannel panicChannel = new NotificationChannel(
                    CHANNEL_ID_PANIC,
                    CHANNEL_NAME_PANIC,
                    NotificationManager.IMPORTANCE_HIGH
            );
            panicChannel.setDescription("Emergency panic alerts from rides");
            panicChannel.enableVibration(true);
            panicChannel.setVibrationPattern(new long[]{0, 500, 200, 500});
            panicChannel.enableLights(true);
            notificationManager.createNotificationChannel(panicChannel);

            // Default Channel
            NotificationChannel defaultChannel = new NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    CHANNEL_NAME_DEFAULT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            defaultChannel.setDescription("Notifications about ride status and assignments");
            notificationManager.createNotificationChannel(defaultChannel);
        }
    }

    /**
     * Show high-priority panic notification.
     */
    private void showPanicNotification(String title, String body, PendingIntent pendingIntent) {
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_PANIC)
                .setSmallIcon(R.drawable.ic_warning)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID_PANIC, builder.build());
        }
    }

    /**
     * Show generic notification.
     */
    private void showGenericNotification(String title, String body, PendingIntent pendingIntent) {
        Uri defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_DEFAULT)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSound(defaultSound)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}
