import { Component, OnInit, Input, Output, EventEmitter, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, AdminRideDetailResponse, LocationPoint } from '../../../core/services/ride-api.service';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';

@Component({
  selector: 'app-admin-ride-detail-modal',
  standalone: true,
  imports: [CommonModule, RideMapComponent],
  templateUrl: './admin-ride-detail-modal.component.html',
  styleUrls: ['./admin-ride-detail-modal.component.css']
})
export class AdminRideDetailModalComponent implements OnInit {
  @Input() rideId!: number;
  @Output() close = new EventEmitter<void>();

  private rideService = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);

  rideDetail?: AdminRideDetailResponse;
  loading = true;
  error?: string;

  // Map data
  routePoints: LocationPoint[] = [];

  ngOnInit(): void {
    this.loadRideDetail();
  }

  loadRideDetail() {
    this.loading = true;
    this.error = undefined;

    this.rideService.getAdminRideDetail(this.rideId).subscribe({
      next: (detail) => {
        this.rideDetail = detail;
        this.parseRouteCoordinates();
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

  parseRouteCoordinates() {
    if (!this.rideDetail?.routeCoordinates) return;

    try {
      const parsed = JSON.parse(this.rideDetail.routeCoordinates);
      // Backend may return array of [lat, lng] or objects with latitude/longitude
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
    return '★'.repeat(rating) + '☆'.repeat(5 - rating);
  }

  getDriverName(): string {
    if (!this.rideDetail?.driver) return 'No driver assigned';
    const d = this.rideDetail.driver;
    return `${d.firstName} ${d.lastName}`;
  }

  getPassengerNames(): string {
    if (!this.rideDetail?.passengers || this.rideDetail.passengers.length === 0) {
      return 'No passengers';
    }
    return this.rideDetail.passengers.map(p => `${p.firstName} ${p.lastName}`).join(', ');
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
