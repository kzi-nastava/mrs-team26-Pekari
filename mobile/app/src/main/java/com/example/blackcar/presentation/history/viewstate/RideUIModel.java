package com.example.blackcar.presentation.history.viewstate;

import java.util.List;
import java.util.Objects;

public class RideUIModel {

    public String startTime;
    public String endTime;
    public String origin;
    public String destination;
    public String canceledBy; // null if not canceled
    public boolean panic;
    public double price;
    public List<String> passengers;

    public RideUIModel() { }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RideUIModel that = (RideUIModel) o;
        return panic == that.panic &&
               Double.compare(that.price, price) == 0 &&
               Objects.equals(startTime, that.startTime) &&
               Objects.equals(endTime, that.endTime) &&
               Objects.equals(origin, that.origin) &&
               Objects.equals(destination, that.destination) &&
               Objects.equals(canceledBy, that.canceledBy) &&
               Objects.equals(passengers, that.passengers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, origin, destination,
                          canceledBy, panic, price, passengers);
    }
}
