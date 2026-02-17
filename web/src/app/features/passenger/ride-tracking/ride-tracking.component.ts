import { Component, OnInit, inject, signal, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { RideApiService, ActiveRideResponse } from '../../../core/services/ride-api.service';
import { WebSocketService } from '../../../core/services/websocket.service';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-ride-tracking',
  standalone: true,
  imports: [CommonModule, FormsModule, RideMapComponent],
  templateUrl: './ride-tracking.component.html',
  styleUrls: ['./ride-tracking.component.css']
})
export class RideTrackingComponent implements OnInit, OnDestroy {
  private route = inject(ActivatedRoute);
  public router = inject(Router);
  private rideApi = inject(RideApiService);
  private ws = inject(WebSocketService);

  ride = signal<ActiveRideResponse | null>(null);
  driverLocation = signal<{latitude: number, longitude: number} | null>(null);
  estimatedTimeMinutes = signal<number | undefined>(undefined);
  showReportForm = signal<boolean>(false);
  reportText = '';
  reportSubmitting = signal<boolean>(false);
  reportSuccess = signal<string | undefined>(undefined);
  error = signal<string | undefined>(undefined);

  private trackingSubscription?: Subscription;

  ngOnInit() {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    if (id) {
      // First, try to fetch the active ride details
      this.rideApi.getActiveRideForPassenger().subscribe({
        next: (res) => {
          if (res && res.rideId === id) {
            this.ride.set(res);
            this.startTracking(id);
          } else {
            console.error('Ride not found or not active', id);
            // Optional: redirect or show error
          }
        },
        error: (err) => {
          console.error('Error fetching ride details', err);
        }
      });
    }
  }

  private startTracking(rideId: number) {
    this.trackingSubscription = this.ws.subscribeToRideTracking(rideId).subscribe(update => {
      if (update.vehicleLatitude && update.vehicleLongitude) {
        this.driverLocation.set({
          latitude: update.vehicleLatitude,
          longitude: update.vehicleLongitude
        });
      }

      if (update.estimatedTimeToDestinationMinutes !== undefined) {
          this.estimatedTimeMinutes.set(update.estimatedTimeToDestinationMinutes);
      }

      if (update.rideStatus === 'COMPLETED') {
          // Ride finished, maybe redirect or show message
          console.log('Ride completed');
      }
    });
  }

  ngOnDestroy() {
    this.trackingSubscription?.unsubscribe();
  }

  get mapPickup() {
    return this.ride()?.pickup || null;
  }

  get mapDropoff() {
    return this.ride()?.dropoff || null;
  }

  get mapStops() {
    return this.ride()?.stops || [];
  }

  get routePoints() {
      // If we have routeCoordinates as string, we might need to parse it
      // But RideMapComponent might handle it.
      // In PassengerHomeComponent it uses estimate.routePoints which is LocationPoint[]
      // ActiveRideResponse has routeCoordinates as string (serialized JSON)
      const coords = this.ride()?.routeCoordinates;
      if (typeof coords === 'string') {
          try {
              return JSON.parse(coords);
          } catch (e) {
              return [];
          }
      }
      return [];
  }

  toggleReportForm() {
    this.showReportForm.update(v => !v);
    this.reportSuccess.set(undefined);
  }

  submitReport() {
    const rideId = this.ride()?.rideId;
    const text = this.reportText.trim();

    if (!rideId || !text) {
      return;
    }

    this.reportSubmitting.set(true);
    this.reportSuccess.set(undefined);
    this.error.set(undefined);

    this.rideApi.reportInconsistency(rideId, text).subscribe({
      next: (response) => {
        this.reportSubmitting.set(false);
        this.reportSuccess.set(response.message || 'Report submitted successfully');
        this.reportText = '';
        this.showReportForm.set(false);
      },
      error: (err) => {
        this.reportSubmitting.set(false);
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error.set(backendMsg || plainMsg || err?.message || 'Failed to submit report');
      }
    });
  }
}
