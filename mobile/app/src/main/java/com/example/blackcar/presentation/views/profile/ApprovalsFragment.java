package com.example.blackcar.presentation.views.profile;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.blackcar.BlackCarApp;
import com.example.blackcar.R;
import com.example.blackcar.databinding.FragmentApprovalsBinding;
import com.example.blackcar.domain.model.ApprovalRequest;
import com.example.blackcar.presentation.viewmodel.AppViewModelFactory;
import com.example.blackcar.presentation.viewmodel.ApprovalsViewModel;

public final class ApprovalsFragment extends Fragment {
    private FragmentApprovalsBinding binding;
    private ApprovalsViewModel viewModel;
    private ApprovalRequestsAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentApprovalsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        BlackCarApp app = (BlackCarApp) requireActivity().getApplication();
        AppViewModelFactory factory = new AppViewModelFactory(app.getAppContainer().getSessionManager(), app.getAppContainer().getProfileRepository());
        viewModel = new ViewModelProvider(this, factory).get(ApprovalsViewModel.class);

        adapter = new ApprovalRequestsAdapter(
                request -> viewModel.approve(request.getId()),
                request -> promptReject(request)
        );
        binding.approvalsList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.approvalsList.setAdapter(adapter);

        viewModel.getState().observe(getViewLifecycleOwner(), state -> {
            if (state.successMessage != null) {
                binding.approvalsSuccess.setVisibility(View.VISIBLE);
                binding.approvalsSuccess.setText(state.successMessage);
            } else {
                binding.approvalsSuccess.setVisibility(View.GONE);
            }
            if (state.errorMessage != null) {
                binding.approvalsError.setVisibility(View.VISIBLE);
                binding.approvalsError.setText(state.errorMessage);
            } else {
                binding.approvalsError.setVisibility(View.GONE);
            }
            adapter.submit(state.requests);
        });

        viewModel.refresh();
    }

    private void promptReject(@NonNull ApprovalRequest request) {
        final android.widget.EditText reason = new android.widget.EditText(requireContext());
        reason.setHint(getString(R.string.rejection_reason) + " (" + getString(R.string.optional) + ")");

        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.reject)
                .setView(reason)
                .setPositiveButton(R.string.reject, (d, which) -> viewModel.reject(request.getId(), reason.getText().toString().trim()))
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
