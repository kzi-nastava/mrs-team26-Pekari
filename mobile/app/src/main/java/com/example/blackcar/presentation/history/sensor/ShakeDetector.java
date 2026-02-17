package com.example.blackcar.presentation.history.sensor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Detects shake gestures using the device's accelerometer.
 * A shake is detected when the acceleration magnitude exceeds the threshold.
 */
public class ShakeDetector implements SensorEventListener {

    /**
     * Interface for shake event callbacks.
     */
    public interface OnShakeListener {
        void onShake();
    }

    // Shake detection threshold - needs to be high enough to ignore rotation/flipping
    private static final float SHAKE_THRESHOLD = 25.0f;

    // Minimum time between shake detections (milliseconds)
    private static final long SHAKE_COOLDOWN_MS = 1000;

    // Minimum number of shake movements required
    private static final int MIN_SHAKE_COUNT = 2;

    private OnShakeListener listener;
    private long lastShakeTime = 0;
    private long lastMovementTime = 0;
    private int shakeCount = 0;

    public ShakeDetector(OnShakeListener listener) {
        this.listener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Calculate total acceleration magnitude and subtract gravity
            // This properly handles gravity regardless of device orientation
            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

            long currentTime = System.currentTimeMillis();

            // Check if acceleration exceeds threshold (true shake, not just rotation)
            if (acceleration > SHAKE_THRESHOLD) {
                // Reset shake count if too much time passed since last movement
                if (currentTime - lastMovementTime > 500) {
                    shakeCount = 0;
                }

                shakeCount++;
                lastMovementTime = currentTime;

                // Only trigger if we have multiple shake movements and cooldown passed
                if (shakeCount >= MIN_SHAKE_COUNT && currentTime - lastShakeTime > SHAKE_COOLDOWN_MS) {
                    lastShakeTime = currentTime;
                    shakeCount = 0;

                    if (listener != null) {
                        listener.onShake();
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used, but required by interface
    }

    public void setListener(OnShakeListener listener) {
        this.listener = listener;
    }
}
