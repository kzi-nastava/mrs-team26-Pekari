package com.example.blackcar.presentation.home;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.blackcar.R;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.databinding.ActivityMainBinding;


import com.google.android.material.bottomnavigation.BottomNavigationView;

import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
}
