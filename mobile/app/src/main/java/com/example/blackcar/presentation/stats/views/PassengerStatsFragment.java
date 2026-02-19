package com.example.blackcar.presentation.stats.views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentPassengerStatsBinding;
import com.example.blackcar.data.api.model.RideStatsDayDto;
import com.example.blackcar.data.api.model.RideStatsResponse;
import com.example.blackcar.presentation.stats.viewmodel.PassengerStatsViewModel;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassengerStatsFragment extends Fragment {

    private FragmentPassengerStatsBinding binding;
    private PassengerStatsViewModel viewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPassengerStatsBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(this).get(PassengerStatsViewModel.class);

        binding.editDateFrom.setText(PassengerStatsViewModel.getDefaultDateFrom());
        binding.editDateTo.setText(PassengerStatsViewModel.getDefaultDateTo());

        binding.editDateFrom.setOnClickListener(v -> openDatePicker(true));
        binding.editDateTo.setOnClickListener(v -> openDatePicker(false));

        binding.btnFilter.setOnClickListener(v -> loadStats());
        binding.btnReset.setOnClickListener(v -> {
            binding.editDateFrom.setText(PassengerStatsViewModel.getDefaultDateFrom());
            binding.editDateTo.setText(PassengerStatsViewModel.getDefaultDateTo());
            loadStats();
        });

        observeState();
        loadStats();

        return binding.getRoot();
    }

    private void openDatePicker(boolean isFrom) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().build();
        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                LocalDate date = Instant.ofEpochMilli(selection).atZone(ZoneId.systemDefault()).toLocalDate();
                String formatted = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
                if (isFrom) {
                    binding.editDateFrom.setText(formatted);
                } else {
                    binding.editDateTo.setText(formatted);
                }
            }
        });
        picker.show(getParentFragmentManager(), "datePicker");
    }

    private void loadStats() {
        String from = binding.editDateFrom.getText() != null ? binding.editDateFrom.getText().toString().trim() : "";
        String to = binding.editDateTo.getText() != null ? binding.editDateTo.getText().toString().trim() : "";
        if (from.isEmpty() || to.isEmpty()) {
            Toast.makeText(requireContext(), "Please select date range", Toast.LENGTH_SHORT).show();
            return;
        }
        viewModel.loadStats(from, to);
    }

    private void observeState() {
        viewModel.getToastMessage().observe(getViewLifecycleOwner(), msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                viewModel.clearToastMessage();
            }
        });

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.progress.setVisibility(state.loading ? View.VISIBLE : View.GONE);

            if (state.error != null) {
                binding.txtError.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.error);
                binding.layoutSummary.setVisibility(View.GONE);
                binding.txtEmpty.setVisibility(View.GONE);
            } else if (state.stats != null) {
                binding.txtError.setVisibility(View.GONE);
                if (state.stats.getDailyData() != null && !state.stats.getDailyData().isEmpty()) {
                    binding.layoutSummary.setVisibility(View.VISIBLE);
                    binding.txtEmpty.setVisibility(View.GONE);
                    bindStats(state.stats);
                } else {
                    binding.layoutSummary.setVisibility(View.GONE);
                    binding.txtEmpty.setVisibility(View.VISIBLE);
                    binding.txtEmpty.setText("No data in selected range");
                }
            } else {
                binding.txtError.setVisibility(View.GONE);
                binding.layoutSummary.setVisibility(View.GONE);
                binding.txtEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void bindStats(RideStatsResponse stats) {
        binding.txtTotalRides.setText(String.valueOf(stats.getTotalRides()));
        binding.txtAvgRides.setText(String.format(Locale.getDefault(), "Avg: %.1f/day", stats.getAvgRidesPerDay()));

        binding.txtTotalDistance.setText(String.format(Locale.getDefault(), "%.1f", stats.getTotalDistanceKm()));
        binding.txtAvgDistance.setText(String.format(Locale.getDefault(), "Avg: %.1f km/day", stats.getAvgDistancePerDay()));

        binding.txtTotalAmount.setText(String.format(Locale.getDefault(), "%.0f", stats.getTotalAmount()));
        binding.txtAvgAmount.setText(String.format(Locale.getDefault(), "Avg: %.0f RSD/day", stats.getAvgAmountPerDay()));

        List<RideStatsDayDto> daily = stats.getDailyData();
        if (daily != null && !daily.isEmpty()) {
            setupBarChart(binding.chartRides, daily, d -> (float) d.getRideCount(), 0xFF22C55E);
            setupBarChart(binding.chartDistance, daily, d -> (float) d.getDistanceKm(), 0xFF3B82F6);
            setupBarChart(binding.chartAmount, daily, d -> (float) d.getAmount(), 0xFFA855F7);
        }
    }

    private interface ValueExtractor {
        float get(RideStatsDayDto d);
    }

    private void setupBarChart(BarChart chart, List<RideStatsDayDto> daily, ValueExtractor extractor, int color) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < daily.size(); i++) {
            RideStatsDayDto d = daily.get(i);
            entries.add(new BarEntry(i, extractor.get(d)));
            labels.add(formatChartLabel(d.getDate()));
        }

        BarDataSet dataSet = new BarDataSet(entries, "");
        dataSet.setColor(color);
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        chart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getXAxis().setTextColor(0xFFA1A1A1);
        chart.getAxisLeft().setTextColor(0xFFA1A1A1);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setExtraBottomOffset(4f);
        chart.invalidate();
    }

    private String formatChartLabel(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr != null ? dateStr : "";
        try {
            LocalDate d = LocalDate.parse(dateStr.substring(0, 10));
            return d.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()));
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
