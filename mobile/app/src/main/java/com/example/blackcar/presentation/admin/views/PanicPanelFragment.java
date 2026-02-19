package com.example.blackcar.presentation.admin.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentPanicPanelBinding;
import com.example.blackcar.presentation.admin.viewmodel.PanicPanelViewModel;
import com.example.blackcar.presentation.admin.viewstate.PanicPanelViewState;

public class PanicPanelFragment extends Fragment {

    private FragmentPanicPanelBinding binding;
    private PanicPanelViewModel viewModel;
    private PanicRideAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPanicPanelBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PanicPanelViewModel.class);

        setupRecyclerView();
        setupClickListeners();
        observeState();
        observeNewPanicAlerts();

        viewModel.loadPanicRides();
    }

    private void setupRecyclerView() {
        adapter = new PanicRideAdapter();
        binding.recyclerPanicRides.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPanicRides.setAdapter(adapter);
    }

    private void setupClickListeners() {
        binding.btnRetry.setOnClickListener(v -> viewModel.loadPanicRides());
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state instanceof PanicPanelViewState.Loading) {
                showLoading();
            } else if (state instanceof PanicPanelViewState.Success) {
                PanicPanelViewState.Success success = (PanicPanelViewState.Success) state;
                showSuccess(success);
            } else if (state instanceof PanicPanelViewState.Error) {
                PanicPanelViewState.Error error = (PanicPanelViewState.Error) state;
                showError(error.getMessage());
            }
        });
    }

    private void observeNewPanicAlerts() {
        viewModel.getNewPanicAlert().observe(getViewLifecycleOwner(), newCount -> {
            if (newCount != null && newCount > 0) {
                String message = newCount == 1
                        ? getString(R.string.panic_new_alert_single)
                        : getString(R.string.panic_new_alert_multiple, newCount);
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutError.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.GONE);
    }

    private void showSuccess(PanicPanelViewState.Success success) {
        binding.progressBar.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.GONE);

        if (success.getPanicRides().isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.layoutContent.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.layoutContent.setVisibility(View.VISIBLE);

            // Update count badge
            int count = success.getPanicRides().size();
            binding.txtPanicCount.setText(String.valueOf(count));
            binding.txtPanicLabel.setText(count == 1
                    ? getString(R.string.panic_alert_single)
                    : getString(R.string.panic_alert_multiple));

            adapter.submitList(success.getPanicRides());
        }
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.layoutEmpty.setVisibility(View.GONE);
        binding.layoutContent.setVisibility(View.GONE);
        binding.layoutError.setVisibility(View.VISIBLE);
        binding.txtError.setText(message);
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.startPolling();
    }

    @Override
    public void onPause() {
        super.onPause();
        viewModel.stopPolling();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
