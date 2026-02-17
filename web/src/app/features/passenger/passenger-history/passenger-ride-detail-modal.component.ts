import { Component, OnInit, Input, Output, EventEmitter, inject, ChangeDetectorRef, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, PassengerRideDetailResponse, LocationPoint } from '../../../core/services/ride-api.service';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';
import { WebSocketService } from '../../../core/services/websocket.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-passenger-ride-detail-modal',
  standalone: true,
  imports: [CommonModule, RideMapComponent],
  templateUrl: './passenger-ride-detail-modal.component.html',
  styleUrls: ['./passenger-ride-detail-modal.component.css']
})
export class PassengerRideDetailModalComponent implements OnInit, OnDestroy {
  @Input() rideId!: number;
  @Output() close = new EventEmitter<void>();

  private rideService = inject(RideApiService);
  private wsService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);

  rideDetail?: PassengerRideDetailResponse;
  loading = true;
  error?: string;

  // Map data
  routePoints: LocationPoint[] = [];
  driverLocation: { latitude: number; longitude: number } | null = null;
  estimatedTimeMinutes?: number;
  private trackingSubscription?: Subscription;

  ngOnInit(): void {
    this.loadRideDetail();
  }

  ngOnDestroy(): void {
    this.stopTracking();
  }

  loadRideDetail() {
    this.loading = true;
    this.error = undefined;

    this.rideService.getPassengerRideDetail(this.rideId).subscribe({
      next: (detail) => {
        this.rideDetail = detail;
        this.parseRouteCoordinates();

        if (this.isActiveForTracking(detail.status)) {
          this.startTracking();
        }

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.message || 'Failed to load ride details';
        this.loading = false;
        console.error('Error loading ride details:', err);
        this.cdr.detectChanges();
      }
    });
  }

  isActiveForTracking(status: string): boolean {
    return status === 'ACCEPTED' || status === 'IN_PROGRESS' || status === 'SCHEDULED' || status === 'STOP_REQUESTED';
  }

  startTracking() {
    this.stopTracking();
    this.wsService.connect();

    this.trackingSubscription = this.wsService.subscribeToRideTracking(this.rideId).subscribe({
      next: (update) => {
        if (update.vehicleLatitude && update.vehicleLongitude) {
          this.driverLocation = {
            latitude: update.vehicleLatitude,
            longitude: update.vehicleLongitude
          };
        }

        if (update.estimatedTimeToDestinationMinutes !== undefined) {
          this.estimatedTimeMinutes = update.estimatedTimeToDestinationMinutes;
        }

        this.cdr.detectChanges();

        if (update.rideStatus === 'COMPLETED') {
          this.stopTracking();
          this.loadRideDetail();
        }
      },
      error: (err) => {
        console.error('Tracking error:', err);
      }
    });
  }

  stopTracking() {
    if (this.trackingSubscription) {
      this.trackingSubscription.unsubscribe();
      this.trackingSubscription = undefined;
    }
  }

  parseRouteCoordinates() {
    if (!this.rideDetail?.routeCoordinates) return;

    try {
      const parsed = JSON.parse(this.rideDetail.routeCoordinates);
      if (Array.isArray(parsed)) {
        this.routePoints = parsed.map((point: any) => {
          if (Array.isArray(point)) {
            return { latitude: point[0], longitude: point[1], address: '' };
          } else {
            return { latitude: point.latitude, longitude: point.longitude, address: '' };
          }
        });
      }
    } catch (e) {
      console.error('Failed to parse route coordinates:', e);
    }
  }

  onClose() {
    this.close.emit();
  }

  onBackdropClick(event: MouseEvent) {
    if ((event.target as HTMLElement).classList.contains('modal-backdrop')) {
      this.onClose();
    }
  }

  // Formatting helpers
  formatDateTime(dateTime: string | null): string {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    return date.toLocaleString('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatPrice(price: number): string {
    return price ? `${price.toLocaleString()} RSD` : '-';
  }

  formatDistance(km: number): string {
    return km ? `${km.toFixed(1)} km` : '-';
  }

  getStars(rating: number): string {
    return '\u2605'.repeat(rating) + '\u2606'.repeat(5 - rating);
  }

  getDriverName(): string {
    if (!this.rideDetail?.driver) return 'No driver assigned';
    const d = this.rideDetail.driver;
    return `${d.firstName} ${d.lastName}`;
  }

  getStatusClass(): string {
    if (!this.rideDetail) return '';
    if (this.rideDetail.panicActivated) return 'status-panic';
    if (this.rideDetail.cancelled) return 'status-cancelled';
    switch (this.rideDetail.status) {
      case 'COMPLETED': return 'status-completed';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'ACCEPTED':
      case 'SCHEDULED': return 'status-scheduled';
      default: return '';
    }
  }

  getStatusLabel(): string {
    if (!this.rideDetail) return '';
    if (this.rideDetail.panicActivated) return 'PANIC ACTIVATED';
    if (this.rideDetail.cancelled) return `Cancelled by ${this.rideDetail.cancelledBy || 'unknown'}`;
    return this.rideDetail.status;
  }
}
