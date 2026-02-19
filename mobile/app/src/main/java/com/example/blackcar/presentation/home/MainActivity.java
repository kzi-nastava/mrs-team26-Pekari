package com.example.blackcar.presentation.home;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.blackcar.R;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.databinding.ActivityMainBinding;


import com.google.android.material.bottomnavigation.BottomNavigationView;

import com.example.blackcar.data.auth.TokenManager;
import com.example.blackcar.data.repository.NotificationRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import android.util.Log;

import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private NavController navController;

    private final androidx.activity.result.ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("MainActivity", "[DEBUG_LOG] Notification permission granted");
                } else {
                    Log.w("MainActivity", "[DEBUG_LOG] Notification permission denied. App will not show notifications.");
                    // Optional: Inform user that the app will not show notifications.
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("MainActivity", "--------------------------------------------------");
        Log.i("MainActivity", "[DEBUG_LOG] MainActivity.onCreate() starting...");
        Log.i("MainActivity", "--------------------------------------------------");

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize ApiClient (safety net; Application also does this)
        com.example.blackcar.data.api.ApiClient.init(getApplicationContext());

        // Check Firebase initialization
        checkFirebaseStatus();

        // Request notification permission for Android 13+
        askNotificationPermission();

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
        }

        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_history) {
                navigateToHistory();
            } else if (id == R.id.nav_dashboard) {
                navController.navigate(R.id.homeFragment);
            } else if (id == R.id.nav_panic) {
                navController.navigate(R.id.panicPanelFragment);
            } else if (id == R.id.nav_profile) {
                navController.navigate(R.id.profileFragment);
            } else {
                return false;
            }
            return true;
        });

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment ||
                destination.getId() == R.id.registerFragment ||
                destination.getId() == R.id.forgotPasswordFragment) {
                binding.bottomNavigation.setVisibility(android.view.View.GONE);
                binding.appBarLayout.setVisibility(android.view.View.GONE);
                setNavHostBottomMargin(0);
            } else {
                binding.bottomNavigation.setVisibility(android.view.View.VISIBLE);
                binding.appBarLayout.setVisibility(android.view.View.VISIBLE);
                updateBottomNavMenuForRole();
                syncBottomNavSelection(destination.getId());
                int bottom = binding.bottomNavigation.getHeight() > 0 ? binding.bottomNavigation.getHeight() : dpToPx(56);
                setNavHostBottomMargin(bottom);
            }
        });

        // Handle deep-link from notification
        handleNotificationIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleNotificationIntent(intent);
    }

    /**
     * Handle intent from FCM notification tap.
     * Navigates to appropriate screen based on intent extras.
     */
    private void handleNotificationIntent(Intent intent) {
        // Sync FCM token on every activity start if logged in
        syncFcmToken();

        if (intent == null) return;

        String navigateTo = intent.getStringExtra("navigate_to");
        if (navigateTo == null) return;

        String rideIdStr = intent.getStringExtra("ride_id");

        binding.getRoot().postDelayed(() -> {
            if (navController == null || SessionManager.getRole() == null) return;

            try {
                if ("panic_panel".equals(navigateTo)) {
                    if (SessionManager.getRole().equalsIgnoreCase("admin")) {
                        navController.navigate(R.id.panicPanelFragment);
                    }
                } else if ("ride_details".equals(navigateTo)) {
                    if (rideIdStr != null) {
                        try {
                            long rideId = Long.parseLong(rideIdStr);
                            Bundle args = new Bundle();
                            args.putLong("rideId", rideId);
                            navController.navigate(R.id.rideTrackingFragment, args);
                        } catch (NumberFormatException e) {
                            // Fallback to home if ID is invalid
                            navController.navigate(R.id.homeFragment);
                        }
                    } else {
                        navController.navigate(R.id.homeFragment);
                    }
                }
            } catch (Exception e) {
                // Navigation may fail if not logged in or destination not available
            }
        }, 300);

        // Clear the intent extra to prevent re-navigation
        intent.removeExtra("navigate_to");
        intent.removeExtra("ride_id");
    }

    private void checkFirebaseStatus() {
        try {
            com.google.firebase.FirebaseApp app = com.google.firebase.FirebaseApp.getInstance();
            Log.v("MainActivity", "[DEBUG_LOG] Firebase initialized: " + app.getName());
        } catch (Exception e) {
            Log.v("MainActivity", "[DEBUG_LOG] Firebase initialization check failed", e);
        }
    }

    private void syncFcmToken() {
        TokenManager tm = TokenManager.getInstance(this);
        String jwt = tm.getToken();
        boolean hasToken = tm.hasToken();
        
        Log.i("MainActivity", "[DEBUG_LOG] syncFcmToken started. JWT present: " + (jwt != null) + ", hasToken(): " + hasToken);

        if (!hasToken) {
            Log.i("MainActivity", "[DEBUG_LOG] Skipping FCM sync: No auth token in TokenManager");
            return;
        }

        Log.i("MainActivity", "[DEBUG_LOG] Requesting FCM token from Firebase...");
        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.e("MainActivity", "[DEBUG_LOG] Fetching FCM token failed", task.getException());
                    if (task.getException() != null) {
                        Log.e("MainActivity", "[DEBUG_LOG] FCM Exception message: " + task.getException().getMessage());
                    }
                    return;
                }

                String token = task.getResult();
                if (token == null || token.isEmpty()) {
                    Log.v("MainActivity", "[DEBUG_LOG] FCM token retrieved but it's null or empty");
                    return;
                }

                Log.i("MainActivity", "[DEBUG_LOG] FCM token retrieved successfully: " + token.substring(0, Math.min(token.length(), 10)) + "...");

                tm.saveFcmToken(token);
                new NotificationRepository().registerToken(token, new NotificationRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.v("MainActivity", "[DEBUG_LOG] FCM token registered with backend successfully from MainActivity");
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.v("MainActivity", "[DEBUG_LOG] Failed to register FCM token with backend from MainActivity: " + error);
                    }
                });
            })
            .addOnFailureListener(e -> {
                Log.e("MainActivity", "[DEBUG_LOG] Firebase getToken() Failure Listener triggered: " + e.getMessage(), e);
            })
            .addOnCanceledListener(() -> {
                Log.w("MainActivity", "[DEBUG_LOG] Firebase getToken() Task was canceled");
            });
    }

    private void setNavHostBottomMargin(int px) {
        android.view.View v = binding.navHostFragment;
        android.view.ViewGroup.MarginLayoutParams lp = (android.view.ViewGroup.MarginLayoutParams) v.getLayoutParams();
        if (lp.bottomMargin != px) {
            lp.bottomMargin = px;
            v.setLayoutParams(lp);
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void updateBottomNavMenuForRole() {
        String role = SessionManager.getRole();
        BottomNavigationView bottomNav = binding.bottomNavigation;
        bottomNav.getMenu().clear();
        if (role != null && role.equalsIgnoreCase("admin")) {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu_admin);
        } else {
            bottomNav.inflateMenu(R.menu.bottom_nav_menu);
        }
        applyProfileTabDangerState();
    }

    /**
     * When the user is blocked, style the Profile tab with danger (red) color,
     * matching the web behavior where the profile nav link turns red.
     * Call this when blocked status may have changed (e.g. after profile loads).
     */
    public void refreshProfileTabDangerState() {
        applyProfileTabDangerState();
    }

    private void applyProfileTabDangerState() {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        MenuItem profileItem = bottomNav.getMenu().findItem(R.id.nav_profile);
        if (profileItem != null) {
            if (SessionManager.isBlocked()) {
                profileItem.setIcon(R.drawable.baseline_person_24_danger);
                bottomNav.setItemIconTintList(null);
            } else {
                profileItem.setIcon(R.drawable.baseline_person_24);
                bottomNav.setItemIconTintList(ColorStateList.valueOf(
                        getResources().getColor(R.color.text_primary, getTheme())));
            }
        }
    }

    private void syncBottomNavSelection(int destinationId) {
        BottomNavigationView bottomNav = binding.bottomNavigation;
        int itemId;
        if (destinationId == R.id.homeFragment) {
            itemId = R.id.nav_dashboard;
        } else if (destinationId == R.id.profileFragment || destinationId == R.id.userManagementFragment || 
                   destinationId == R.id.addDriverFragment || destinationId == R.id.pricingManagementFragment) {
            itemId = R.id.nav_profile;
        } else if (destinationId == R.id.panicPanelFragment) {
            itemId = R.id.nav_panic;
        } else if (destinationId == R.id.adminHistoryFragment || destinationId == R.id.driverHistoryFragment || destinationId == R.id.passengerHistoryFragment) {
            itemId = R.id.nav_history;
        } else {
            return;
        }
        MenuItem item = bottomNav.getMenu().findItem(itemId);
        if (item != null && !item.isChecked()) {
            // Use setChecked to avoid triggering OnItemSelectedListener (which would cause
            // a navigation loop when syncing after destination change)
            item.setChecked(true);
        }
    }

    private void navigateToHistory() {
        String role = SessionManager.getRole();
        if (role != null && role.equalsIgnoreCase("admin")) {
            navController.navigate(R.id.adminHistoryFragment);
        } else if (role != null && role.equalsIgnoreCase("driver")) {
            navController.navigate(R.id.driverHistoryFragment);
        } else {
            navController.navigate(R.id.passengerHistoryFragment);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }

    /**
     * Request notification permission for Android 13+ (API 33+).
     */
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                Log.v("MainActivity", "[DEBUG_LOG] POST_NOTIFICATIONS permission already granted");
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                Log.v("MainActivity", "[DEBUG_LOG] Showing rationale for POST_NOTIFICATIONS");
                // In a real app, we would show an educational UI here.
                // For now, we directly ask again or log that we should show a dialog.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                Log.v("MainActivity", "[DEBUG_LOG] Requesting POST_NOTIFICATIONS permission");
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
}
