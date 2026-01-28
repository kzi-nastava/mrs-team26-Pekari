package com.pekara.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "driver_states")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverState {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Column(nullable = false)
    @Builder.Default
    private Boolean online = false;

    @Column(nullable = false)
    @Builder.Default
    private Boolean busy = false;

    @Column
    private Double latitude;

    @Column
    private Double longitude;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "current_ride_ends_at")
    private LocalDateTime currentRideEndsAt;

    @Column(name = "current_ride_end_latitude")
    private Double currentRideEndLatitude;

    @Column(name = "current_ride_end_longitude")
    private Double currentRideEndLongitude;

    @Column(name = "next_scheduled_ride_at")
    private LocalDateTime nextScheduledRideAt;

    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
