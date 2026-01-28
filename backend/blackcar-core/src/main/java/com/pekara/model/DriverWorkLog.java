package com.pekara.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Tracks driver work time for the 8-hour limit enforcement.
 * 
 * Work logs are created when a ride is assigned and updated when the ride completes.
 * Only completed work logs (completed=true) count towards the 8-hour limit.
 */
@Entity
@Table(name = "driver_work_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DriverWorkLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_user_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id")
    private Ride ride;

    /**
     * When the driver started working on this ride (actual start time).
     */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /**
     * When the driver finished this ride (actual end time).
     * Null if the ride hasn't completed yet.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /**
     * Whether the ride was actually completed.
     * Only completed rides count towards the 8-hour work limit.
     * False for: pending rides, cancelled rides, rejected rides.
     */
    @Column(name = "completed", nullable = false)
    @Builder.Default
    private Boolean completed = false;

    /**
     * When this log entry was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
