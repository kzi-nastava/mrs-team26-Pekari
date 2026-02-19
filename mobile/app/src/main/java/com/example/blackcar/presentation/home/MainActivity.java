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

import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 1001;

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Request notification permission for Android 13+
        requestNotificationPermission();

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
            } else if (id == R.id.nav_add_driver) {
                navController.navigate(R.id.addDriverFragment);
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
     * Navigates to PanicPanel if notification was a panic alert.
     */
    private void handleNotificationIntent(Intent intent) {
        if (intent == null) return;

        String navigateTo = intent.getStringExtra("navigate_to");
        if ("panic_panel".equals(navigateTo)) {
            // Navigate to Panic Panel after a short delay to ensure NavController is ready
            binding.getRoot().postDelayed(() -> {
                if (navController != null && SessionManager.getRole() != null
                        && SessionManager.getRole().equalsIgnoreCase("admin")) {
                    try {
                        navController.navigate(R.id.panicPanelFragment);
                    } catch (Exception e) {
                        // Navigation may fail if not logged in or destination not available
                    }
                }
            }, 300);

            // Clear the intent extra to prevent re-navigation
            intent.removeExtra("navigate_to");
        }
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
        } else if (destinationId == R.id.profileFragment || destinationId == R.id.userManagementFragment) {
            itemId = R.id.nav_profile;
        } else if (destinationId == R.id.addDriverFragment) {
            itemId = R.id.nav_add_driver;
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
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }
}
