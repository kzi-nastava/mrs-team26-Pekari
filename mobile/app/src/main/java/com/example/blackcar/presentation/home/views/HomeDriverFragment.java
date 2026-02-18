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
import com.example.blackcar.databinding.FragmentHomeDriverBinding;

public class HomeDriverFragment extends Fragment {

    private FragmentHomeDriverBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeDriverBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.btnViewHistory.setOnClickListener(v -> {
            String role = SessionManager.getRole();
            if (role != null && role.equalsIgnoreCase("admin")) {
                Navigation.findNavController(v).navigate(R.id.adminHistoryFragment);
            } else if (role != null && role.equalsIgnoreCase("driver")) {
                Navigation.findNavController(v).navigate(R.id.driverHistoryFragment);
            } else {
                Navigation.findNavController(v).navigate(R.id.passengerHistoryFragment);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
