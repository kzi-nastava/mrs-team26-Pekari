package com.example.blackcar.presentation.history.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.blackcar.data.api.model.DriverRideHistoryResponse;
import com.example.blackcar.data.api.model.PaginatedResponse;
import com.example.blackcar.data.repository.RideRepository;
import com.example.blackcar.presentation.history.viewstate.DriverHistoryViewState;
import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DriverHistoryViewModel extends ViewModel {

    private final MutableLiveData<DriverHistoryViewState> state = new MutableLiveData<>();
    private final RideRepository rideRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public DriverHistoryViewModel() {
        this.rideRepository = new RideRepository();
    }

    public LiveData<DriverHistoryViewState> getState() {
        return state;
    }


    public void loadHistory(LocalDate from, LocalDate to) {
        state.setValue(new DriverHistoryViewState(true, false, null, new ArrayList<>()));

        String startDate = (from != null) ? from.format(DATE_FORMATTER) : LocalDate.now().minusYears(1).format(DATE_FORMATTER);
        String endDate = (to != null) ? to.format(DATE_FORMATTER) : LocalDate.now().format(DATE_FORMATTER);

        rideRepository.getDriverRideHistory(startDate, endDate, 0, 100,
                new RideRepository.RepoCallback<PaginatedResponse<DriverRideHistoryResponse>>() {
                    @Override
                    public void onSuccess(PaginatedResponse<DriverRideHistoryResponse> data) {
                        List<RideUIModel> uiModels = mapToUIModels(data.getContent());
                        state.setValue(new DriverHistoryViewState(
                                false,
                                false,
                                null,
                                uiModels
                        ));
                    }

                    @Override
                    public void onError(String message) {
                        state.setValue(new DriverHistoryViewState(
                                false,
                                true,
                                message,
                                new ArrayList<>()
                        ));
                    }
                });
    }


    private List<RideUIModel> mapToUIModels(List<DriverRideHistoryResponse> responses) {
        if (responses == null) {
            return new ArrayList<>();
        }

        return responses.stream()
                .map(this::mapToUIModel)
                .collect(Collectors.toList());
    }

    private RideUIModel mapToUIModel(DriverRideHistoryResponse response) {
        RideUIModel model = new RideUIModel();
        model.startTime = response.getStartTime();
        model.endTime = response.getEndTime();
        model.origin = response.getPickupLocation();
        model.destination = response.getDropoffLocation();
        model.canceledBy = (response.getCancelled() != null && response.getCancelled())
                ? response.getCancelledBy()
                : null;
        model.panic = response.getPanicActivated() != null && response.getPanicActivated();
        model.price = response.getPrice() != null ? response.getPrice().doubleValue() : 0.0;

        if (response.getPassengers() != null) {
            model.passengers = response.getPassengers().stream()
                    .map(p -> p.getFirstName() + " " + p.getLastName())
                    .collect(Collectors.toList());
        } else {
            model.passengers = new ArrayList<>();
        }

        return model;
    }

    public void clearFilter() {
        loadHistory(null, null);
    }
}
