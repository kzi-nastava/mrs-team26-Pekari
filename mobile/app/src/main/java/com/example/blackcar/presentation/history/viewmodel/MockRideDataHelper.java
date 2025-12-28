package com.example.blackcar.presentation.history.viewmodel;

import com.example.blackcar.presentation.history.viewstate.RideUIModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MockRideDataHelper {

    public static List<RideUIModel> generateMockRides() {
        List<RideUIModel> rides = new ArrayList<>();

        RideUIModel ride1 = new RideUIModel();
        ride1.startTime = "2024-12-15 08:30";
        ride1.endTime = "2024-12-15 09:15";
        ride1.origin = "City Center";
        ride1.destination = "Airport";
        ride1.price = 2500.00;
        ride1.panic = false;
        ride1.canceledBy = null;
        ride1.passengers = Arrays.asList("Marko Petrović", "Ana Jovanović");
        rides.add(ride1);

        RideUIModel ride2 = new RideUIModel();
        ride2.startTime = "2024-12-18 14:20";
        ride2.endTime = "2024-12-18 14:25";
        ride2.origin = "Railway Station";
        ride2.destination = "Shopping Mall";
        ride2.price = 0.00;
        ride2.panic = false;
        ride2.canceledBy = "Passenger";
        ride2.passengers = Arrays.asList("Nikola Popović");
        rides.add(ride2);

        RideUIModel ride3 = new RideUIModel();
        ride3.startTime = "2024-12-20 22:15";
        ride3.endTime = "2024-12-20 22:45";
        ride3.origin = "Restaurant District";
        ride3.destination = "Residential Area";
        ride3.price = 1800.50;
        ride3.panic = true;
        ride3.canceledBy = null;
        ride3.passengers = Arrays.asList("Jelena Đorđević");
        rides.add(ride3);

        RideUIModel ride4 = new RideUIModel();
        ride4.startTime = "2024-12-22 10:00";
        ride4.endTime = "2024-12-22 10:05";
        ride4.origin = "Downtown";
        ride4.destination = "University Campus";
        ride4.price = 0.00;
        ride4.panic = false;
        ride4.canceledBy = "Driver";
        ride4.passengers = Arrays.asList("Stefan Nikolić");
        rides.add(ride4);

        RideUIModel ride5 = new RideUIModel();
        ride5.startTime = "2024-12-24 18:30";
        ride5.endTime = "2024-12-24 19:00";
        ride5.origin = "Hotel Plaza";
        ride5.destination = "Concert Hall";
        ride5.price = 3200.00;
        ride5.panic = false;
        ride5.canceledBy = null;
        ride5.passengers = Arrays.asList("Ivan Savić", "Milica Todorović", "Dušan Ilić");
        rides.add(ride5);

        RideUIModel ride6 = new RideUIModel();
        ride6.startTime = "2024-12-26 07:15";
        ride6.endTime = "2024-12-26 07:16";
        ride6.origin = "Suburb North";
        ride6.destination = "Business District";
        ride6.price = 0.00;
        ride6.panic = false;
        ride6.canceledBy = "System";
        ride6.passengers = Arrays.asList("Aleksandar Kostić");
        rides.add(ride6);

        RideUIModel ride7 = new RideUIModel();
        ride7.startTime = "2024-12-28 12:00";
        ride7.endTime = "2024-12-28 12:25";
        ride7.origin = "Medical Center";
        ride7.destination = "Park Avenue";
        ride7.price = 1450.00;
        ride7.panic = false;
        ride7.canceledBy = null;
        ride7.passengers = Arrays.asList("Jovana Mitrović");
        rides.add(ride7);

        RideUIModel ride8 = new RideUIModel();
        ride8.startTime = "2024-12-30 23:45";
        ride8.endTime = "2024-12-31 00:15";
        ride8.origin = "Nightclub District";
        ride8.destination = "Apartment Complex East";
        ride8.price = 2100.00;
        ride8.panic = true;
        ride8.canceledBy = null;
        ride8.passengers = Arrays.asList("Nemanja Janković", "Teodora Vasić");
        rides.add(ride8);

        RideUIModel ride9 = new RideUIModel();
        ride9.startTime = "2025-01-02 09:30";
        ride9.endTime = "2025-01-02 10:00";
        ride9.origin = "Airport";
        ride9.destination = "Tech Park";
        ride9.price = 2800.00;
        ride9.panic = false;
        ride9.canceledBy = null;
        ride9.passengers = Arrays.asList("Lazar Stojanović");
        rides.add(ride9);

        RideUIModel ride10 = new RideUIModel();
        ride10.startTime = "2025-01-05 16:20";
        ride10.endTime = "2025-01-05 16:22";
        ride10.origin = "Library";
        ride10.destination = "Coffee Shop Central";
        ride10.price = 0.00;
        ride10.panic = false;
        ride10.canceledBy = "Passenger";
        ride10.passengers = Arrays.asList("Sofija Đukić");
        rides.add(ride10);

        RideUIModel ride11 = new RideUIModel();
        ride11.startTime = "2025-01-08 11:15";
        ride11.endTime = "2025-01-08 11:45";
        ride11.origin = "Gym Center";
        ride11.destination = "Office Building North";
        ride11.price = 1650.00;
        ride11.panic = false;
        ride11.canceledBy = null;
        ride11.passengers = Arrays.asList("Miloš Radovanović");
        rides.add(ride11);

        RideUIModel ride12 = new RideUIModel();
        ride12.startTime = "2025-01-10 19:00";
        ride12.endTime = "2025-01-10 19:35";
        ride12.origin = "Stadium";
        ride12.destination = "Old Town";
        ride12.price = 2200.00;
        ride12.panic = false;
        ride12.canceledBy = null;
        ride12.passengers = Arrays.asList("Petar Stanković", "Dragana Lazić", "Tijana Marković");
        rides.add(ride12);

        RideUIModel ride13 = new RideUIModel();
        ride13.startTime = "2025-01-12 06:45";
        ride13.endTime = "2025-01-12 06:50";
        ride13.origin = "Industrial Zone";
        ride13.destination = "Warehouse District";
        ride13.price = 0.00;
        ride13.panic = false;
        ride13.canceledBy = "Driver";
        ride13.passengers = Arrays.asList("Vladan Simić");
        rides.add(ride13);

        RideUIModel ride14 = new RideUIModel();
        ride14.startTime = "2025-01-15 15:30";
        ride14.endTime = "2025-01-15 16:10";
        ride14.origin = "Botanical Garden";
        ride14.destination = "Marina Bay";
        ride14.price = 3500.00;
        ride14.panic = false;
        ride14.canceledBy = null;
        ride14.passengers = Arrays.asList("Katarina Pavlović", "Ognjen Mladenović");
        rides.add(ride14);

        RideUIModel ride15 = new RideUIModel();
        ride15.startTime = "2025-01-18 13:20";
        ride15.endTime = "2025-01-18 13:50";
        ride15.origin = "Train Station";
        ride15.destination = "City Hall";
        ride15.price = 1900.00;
        ride15.panic = true;
        ride15.canceledBy = null;
        ride15.passengers = Arrays.asList("Kristina Đorđević");
        rides.add(ride15);

        return rides;
    }
}
