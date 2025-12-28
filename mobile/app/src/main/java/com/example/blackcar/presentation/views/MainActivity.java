package com.example.blackcar.presentation.views;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.blackcar.BlackCarApp;
import com.example.blackcar.R;
import com.example.blackcar.databinding.ActivityMainBinding;
import com.example.blackcar.domain.model.UserRole;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        UserRole role = ((BlackCarApp) getApplication()).getAppContainer().getSessionManager().getRole();
        MenuItem approvals = menu.findItem(R.id.action_approvals);
        if (approvals != null) {
            approvals.setVisible(role == UserRole.ADMIN);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment == null) {
            return super.onOptionsItemSelected(item);
        }
        NavController navController = navHostFragment.getNavController();

        int id = item.getItemId();
        if (id == R.id.action_profile) {
            navController.navigate(R.id.profileFragment);
            return true;
        }
        if (id == R.id.action_approvals) {
            navController.navigate(R.id.approvalsFragment);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
