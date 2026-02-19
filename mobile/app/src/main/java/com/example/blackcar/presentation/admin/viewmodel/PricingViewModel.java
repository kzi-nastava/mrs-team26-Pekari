package com.example.blackcar.presentation.admin.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.PricingResponse;
import com.example.blackcar.data.repository.AdminRepository;
import com.example.blackcar.presentation.admin.viewstate.PricingViewState;

import java.util.List;

public class PricingViewModel extends ViewModel {

    private final MutableLiveData<PricingViewState> state = new MutableLiveData<>(PricingViewState.idle());
    private final AdminRepository repository;

    public PricingViewModel() {
        this.repository = new AdminRepository();
    }

    public LiveData<PricingViewState> getState() {
        return state;
    }

    public void loadPricing() {
        state.setValue(state.getValue().withLoading(true));
        repository.getPricing(new AdminRepository.RepoCallback<List<PricingResponse>>() {
            @Override
            public void onSuccess(List<PricingResponse> data) {
                state.postValue(state.getValue().withPricingList(data));
            }

            @Override
            public void onError(String message) {
                state.postValue(state.getValue().withError(message));
            }
        });
    }

    public void startEdit(PricingResponse pricing) {
        // Create a copy for editing
        PricingResponse copy = new PricingResponse(pricing.getVehicleType(), pricing.getBasePrice(), pricing.getPricePerKm());
        state.setValue(state.getValue().withEditingPricing(copy));
    }

    public void cancelEdit() {
        state.setValue(state.getValue().withEditingPricing(null));
    }

    public void savePricing(double newBasePrice) {
        PricingResponse editing = state.getValue().editingPricing;
        if (editing == null) return;

        editing.setBasePrice(newBasePrice);
        state.setValue(state.getValue().withLoading(true));

        repository.updatePricing(editing, new AdminRepository.RepoCallback<PricingResponse>() {
            @Override
            public void onSuccess(PricingResponse data) {
                state.postValue(state.getValue().withSuccess("Pricing updated successfully").withEditingPricing(null));
                loadPricing(); // Refresh list
            }

            @Override
            public void onError(String message) {
                state.postValue(state.getValue().withError(message));
            }
        });
    }

    public void clearMessages() {
        PricingViewState current = state.getValue();
        if (current != null) {
            state.setValue(new PricingViewState(
                current.loading,
                false,
                null,
                null,
                current.pricingList,
                current.editingPricing
            ));
        }
    }
}
