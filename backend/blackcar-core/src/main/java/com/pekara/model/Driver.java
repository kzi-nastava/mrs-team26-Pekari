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

    @Column(name = "vehicle_registration", length = 20)
    private String vehicleRegistration;

    @Column(name = "vehicle_type", length = 30)
    private String vehicleType;
}
