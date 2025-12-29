package com.example.blackcar.presentation.home;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.blackcar.R;
import com.example.blackcar.databinding.ActivityMainBinding;


import com.google.android.material.bottomnavigation.BottomNavigationView;

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
                navController.navigate(R.id.driverHistoryFragment);
            } else if (id == R.id.nav_dashboard) {
                navController.navigate(R.id.homeFragment);
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
            } else {
                binding.bottomNavigation.setVisibility(android.view.View.VISIBLE);
                binding.appBarLayout.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController != null && navController.navigateUp() || super.onSupportNavigateUp();
    }
}
