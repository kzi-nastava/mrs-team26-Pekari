package com.example.blackcar.presentation.history.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.databinding.FragmentDriverHistoryBinding;
import com.example.blackcar.presentation.history.viewmodel.DriverHistoryViewModel;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class DriverHistoryFragment extends Fragment {

    private FragmentDriverHistoryBinding binding;
    private DriverHistoryViewModel viewModel;
    private DriverHistoryAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentDriverHistoryBinding.inflate(inflater, container, false);

        viewModel = new ViewModelProvider(this).get(DriverHistoryViewModel.class);

        setupRecycler();
        setupFilter();

        observeState();

        viewModel.loadHistory(null, null);

        return binding.getRoot();
    }

    private void setupRecycler() {
        adapter = new DriverHistoryAdapter();
        binding.recyclerHistory.setAdapter(adapter);
    }

    private void setupFilter() {
        binding.btnFilter.setOnClickListener(v -> openDatePicker());
    }

    private void openDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> picker = MaterialDatePicker
                .Builder
                .dateRangePicker()
                .setTitleText("Filter by date")
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                Long startMillis = selection.first;
                Long endMillis = selection.second;

                if (startMillis != null && endMillis != null) {
                    LocalDate fromDate = Instant.ofEpochMilli(startMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    LocalDate toDate = Instant.ofEpochMilli(endMillis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate();

                    viewModel.loadHistory(fromDate, toDate);
                }
            }
        });

        picker.addOnNegativeButtonClickListener(dialog -> {
        });

        picker.show(getParentFragmentManager(), "datePicker");
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {

            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);

            if (state.error) {
                binding.errorLayout.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.errorMessage);
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerHistory.setVisibility(View.GONE);
            } else if (state.rides == null || state.rides.isEmpty()) {
                binding.errorLayout.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerHistory.setVisibility(View.GONE);
            } else {
                binding.errorLayout.setVisibility(View.GONE);
                binding.layoutEmptyState.setVisibility(View.GONE);
                binding.recyclerHistory.setVisibility(View.VISIBLE);
            }

            // Update adapter with filtered rides
            adapter.submitList(state.rides);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
