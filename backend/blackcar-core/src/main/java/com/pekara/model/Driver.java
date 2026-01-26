package com.pekara.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("DRIVER")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Driver extends User {

    @Column(name = "license_number", unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry", length = 20)
    private String licenseExpiry;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;

    @Column(name = "vehicle_model", length = 100)
    private String vehicleModel;

    @Column(name = "vehicle_license_plate", length = 30, unique = true)
    private String licensePlate;

    @Column(name = "vehicle_number_of_seats")
    private Integer numberOfSeats;

    @Column(name = "vehicle_baby_friendly")
    private Boolean babyFriendly;

    @Column(name = "vehicle_pet_friendly")
    private Boolean petFriendly;
}
