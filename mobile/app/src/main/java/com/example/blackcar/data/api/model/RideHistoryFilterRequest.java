package com.example.blackcar.data.api.model;

public class RideHistoryFilterRequest {

    private String startDate;
    private String endDate;

    public RideHistoryFilterRequest() {}

    public RideHistoryFilterRequest(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
