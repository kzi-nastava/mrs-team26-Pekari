package com.example.blackcar.presentation.home.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.blackcar.R;
import com.example.blackcar.data.session.SessionManager;
import com.example.blackcar.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        binding.btnViewHistory.setOnClickListener(v -> {
            String role = SessionManager.getRole();
            int actionId;
            if (role != null && role.equalsIgnoreCase("admin")) {
                actionId = R.id.action_home_to_admin_history;
            } else if (role != null && role.equalsIgnoreCase("driver")) {
                actionId = R.id.action_home_to_driver_history;
            } else {
                actionId = R.id.action_home_to_passenger_history;
            }
            Navigation.findNavController(v).navigate(actionId);
        });

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
