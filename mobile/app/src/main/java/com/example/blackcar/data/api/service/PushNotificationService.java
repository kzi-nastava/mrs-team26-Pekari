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
    private static final int NOTIFICATION_ID_PANIC = 1001;

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "FCM token refreshed: " + token);

        // Save token locally
        TokenManager tokenManager = TokenManager.getInstance(getApplicationContext());
        tokenManager.saveFcmToken(token);

        // If user is logged in, register the new token with backend
        if (tokenManager.hasToken()) {
            NotificationRepository repository = new NotificationRepository();
            repository.registerToken(token, new NotificationRepository.RegistrationCallback() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "New FCM token registered with backend");
                }

                @Override
                public void onFailure(String error) {
                    Log.e(TAG, "Failed to register new FCM token: " + error);
                }
            });
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // Check if this is a panic notification
        Map<String, String> data = remoteMessage.getData();
        String type = data.get("type");

        if ("PANIC".equals(type)) {
            handlePanicNotification(remoteMessage, data);
        } else {
            // Handle other notification types
            handleGenericNotification(remoteMessage);
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
        createNotificationChannel();
        showPanicNotification(title, body, pendingIntent);
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

        showGenericNotification(title, body, pendingIntent);
    }

    /**
     * Create notification channel for panic alerts (Android 8+).
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_PANIC,
                    CHANNEL_NAME_PANIC,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Emergency panic alerts from rides");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 200, 500});
            channel.enableLights(true);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
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

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID_PANIC)
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
