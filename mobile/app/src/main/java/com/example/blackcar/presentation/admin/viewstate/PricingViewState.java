package com.example.blackcar.presentation.admin.viewstate;

import com.example.blackcar.data.api.model.PricingResponse;
import java.util.List;

public class PricingViewState {
    public final boolean loading;
    public final boolean error;
    public final String errorMessage;
    public final String successMessage;
    public final List<PricingResponse> pricingList;
    public final PricingResponse editingPricing;

    public PricingViewState(boolean loading, boolean error, String errorMessage, String successMessage, 
                            List<PricingResponse> pricingList, PricingResponse editingPricing) {
        this.loading = loading;
        this.error = error;
        this.errorMessage = errorMessage;
        this.successMessage = successMessage;
        this.pricingList = pricingList;
        this.editingPricing = editingPricing;
    }

    public static PricingViewState idle() {
        return new PricingViewState(false, false, null, null, null, null);
    }

    public PricingViewState copyWith(boolean loading, boolean error, String errorMessage, String successMessage, 
                                     List<PricingResponse> pricingList, PricingResponse editingPricing) {
        return new PricingViewState(
            loading,
            error,
            errorMessage != null ? errorMessage : this.errorMessage,
            successMessage != null ? successMessage : this.successMessage,
            pricingList != null ? pricingList : this.pricingList,
            editingPricing != null ? editingPricing : this.editingPricing
        );
    }
    
    public PricingViewState withLoading(boolean loading) {
        return new PricingViewState(loading, false, null, null, this.pricingList, this.editingPricing);
    }
    
    public PricingViewState withError(String message) {
        return new PricingViewState(false, true, message, null, this.pricingList, this.editingPricing);
    }
    
    public PricingViewState withSuccess(String message) {
        return new PricingViewState(false, false, null, message, this.pricingList, this.editingPricing);
    }
    
    public PricingViewState withPricingList(List<PricingResponse> list) {
        return new PricingViewState(false, false, null, null, list, this.editingPricing);
    }
    
    public PricingViewState withEditingPricing(PricingResponse pricing) {
        return new PricingViewState(false, false, null, null, this.pricingList, pricing);
    }
}
