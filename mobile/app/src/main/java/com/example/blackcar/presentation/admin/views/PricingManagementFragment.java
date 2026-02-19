package com.example.blackcar.presentation.admin.views;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.blackcar.data.api.model.PricingResponse;
import com.example.blackcar.databinding.DialogEditPricingBinding;
import com.example.blackcar.databinding.FragmentPricingManagementBinding;
import com.example.blackcar.presentation.admin.viewmodel.PricingViewModel;

import java.util.Locale;

public class PricingManagementFragment extends Fragment {

    private FragmentPricingManagementBinding binding;
    private PricingViewModel viewModel;
    private PricingAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPricingManagementBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(PricingViewModel.class);

        setupToolbar();
        setupRecyclerView();
        observeState();

        viewModel.loadPricing();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> Navigation.findNavController(requireView()).popBackStack());
    }

    private void setupRecyclerView() {
        adapter = new PricingAdapter(pricing -> viewModel.startEdit(pricing));
        binding.recyclerPricing.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerPricing.setAdapter(adapter);
    }

    private void observeState() {
        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            binding.progressBar.setVisibility(state.loading ? View.VISIBLE : View.GONE);
            
            if (state.error && state.errorMessage != null) {
                binding.txtError.setVisibility(View.VISIBLE);
                binding.txtError.setText(state.errorMessage);
            } else {
                binding.txtError.setVisibility(View.GONE);
            }

            if (state.pricingList != null) {
                adapter.submitList(state.pricingList);
            }

            if (state.successMessage != null) {
                Toast.makeText(requireContext(), state.successMessage, Toast.LENGTH_SHORT).show();
                viewModel.clearMessages();
            }

            if (state.editingPricing != null) {
                showEditDialog(state.editingPricing);
            }
        });
    }

    private void showEditDialog(PricingResponse pricing) {
        DialogEditPricingBinding dialogBinding = DialogEditPricingBinding.inflate(getLayoutInflater());
        
        dialogBinding.txtDialogTitle.setText("Edit " + pricing.getVehicleType() + " Pricing");
        dialogBinding.editBasePrice.setText(String.format(Locale.US, "%.2f", pricing.getBasePrice()));
        dialogBinding.editPricePerKm.setText(String.format(Locale.US, "%.2f", pricing.getPricePerKm()));

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        dialogBinding.btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
            viewModel.cancelEdit();
        });

        dialogBinding.btnSave.setOnClickListener(v -> {
            String priceStr = dialogBinding.editBasePrice.getText().toString();
            try {
                double newPrice = Double.parseDouble(priceStr);
                if (newPrice < 0) {
                    dialogBinding.inputLayoutBasePrice.setError("Price cannot be negative");
                    return;
                }
                viewModel.savePricing(newPrice);
                dialog.dismiss();
            } catch (NumberFormatException e) {
                dialogBinding.inputLayoutBasePrice.setError("Invalid price format");
            }
        });

        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
