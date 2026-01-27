import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, ActiveRideResponse } from '../../../core/services/ride-api.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-driver-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-home.component.html',
  styleUrls: ['./driver-home.component.css']
})
export class DriverHomeComponent implements OnInit {
  private rideService = inject(RideApiService);
  private router = inject(Router);

  activeRide = signal<ActiveRideResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionInProgress = signal(false);

  ngOnInit() {
    this.loadActiveRide();
  }

  loadActiveRide() {
    this.loading.set(true);
    this.error.set(null);

    this.rideService.getActiveRideForDriver().subscribe({
      next: (ride) => {
        this.activeRide.set(ride);
        this.loading.set(false);
      },
      error: (err) => {
        if (err.status === 204) {
          // No active ride
          this.activeRide.set(null);
        } else {
          this.error.set('Failed to load active ride. Please try again.');
          console.error('Error loading active ride:', err);
        }
        this.loading.set(false);
      }
    });
  }

  startRide() {
    const ride = this.activeRide();
    if (!ride) return;

    if (confirm('Are you sure all passengers have boarded the vehicle?')) {
      this.actionInProgress.set(true);
      this.error.set(null);

      this.rideService.startRide(ride.rideId).subscribe({
        next: () => {
          this.actionInProgress.set(false);
          this.loadActiveRide(); // Reload to get updated status
        },
        error: (err) => {
          this.actionInProgress.set(false);
          this.error.set(err.error?.message || 'Failed to start ride. Please try again.');
          console.error('Error starting ride:', err);
        }
      });
    }
  }

  completeRide() {
    const ride = this.activeRide();
    if (!ride) return;

    if (confirm('Are you sure you want to complete this ride?')) {
      this.actionInProgress.set(true);
      this.error.set(null);

      this.rideService.completeRide(ride.rideId).subscribe({
        next: () => {
          this.actionInProgress.set(false);
          this.loadActiveRide(); // Reload to check for new rides
        },
        error: (err) => {
          this.actionInProgress.set(false);
          this.error.set(err.error?.message || 'Failed to complete ride. Please try again.');
          console.error('Error completing ride:', err);
        }
      });
    }
  }

  cancelRide() {
    const ride = this.activeRide();
    if (!ride) return;

    const reason = prompt('Please provide a reason for cancellation:');
    if (reason && reason.trim()) {
      this.actionInProgress.set(true);
      this.error.set(null);

      this.rideService.cancelRide(ride.rideId, reason).subscribe({
        next: () => {
          this.actionInProgress.set(false);
          this.loadActiveRide(); // Reload to check for new rides
        },
        error: (err) => {
          this.actionInProgress.set(false);
          this.error.set(err.error?.message || 'Failed to cancel ride. Please try again.');
          console.error('Error cancelling ride:', err);
        }
      });
    }
  }

  canStartRide(): boolean {
    const ride = this.activeRide();
    return ride !== null && (ride.status === 'ACCEPTED' || ride.status === 'SCHEDULED');
  }

  canCompleteRide(): boolean {
    const ride = this.activeRide();
    return ride !== null && ride.status === 'IN_PROGRESS';
  }

  canCancelRide(): boolean {
    const ride = this.activeRide();
    return ride !== null && ride.status !== 'COMPLETED' && ride.status !== 'CANCELLED';
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACCEPTED':
        return 'Assigned - Waiting to Start';
      case 'SCHEDULED':
        return 'Scheduled';
      case 'IN_PROGRESS':
        return 'In Progress';
      case 'COMPLETED':
        return 'Completed';
      case 'CANCELLED':
        return 'Cancelled';
      default:
        return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACCEPTED':
        return 'status-assigned';
      case 'SCHEDULED':
        return 'status-scheduled';
      case 'IN_PROGRESS':
        return 'status-in-progress';
      case 'COMPLETED':
        return 'status-completed';
      case 'CANCELLED':
        return 'status-cancelled';
      default:
        return '';
    }
  }
}
