import { Component, OnInit, OnDestroy, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, ActiveRideResponse } from '../../../core/services/ride-api.service';
import { Router } from '@angular/router';
import * as L from 'leaflet';

@Component({
  selector: 'app-driver-home',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './driver-home.component.html',
  styleUrls: ['./driver-home.component.css']
})
export class DriverHomeComponent implements OnInit, OnDestroy {
  private rideService = inject(RideApiService);
  private router = inject(Router);
  private map: L.Map | null = null;
  private carMarker: L.Marker | null = null;
  private routePolyline: L.Polyline | null = null;
  private simulationTimer: ReturnType<typeof setInterval> | null = null;
  private routePoints: L.LatLngTuple[] = [];
  private currentRouteIndex = 0;
  private readonly carIcon = L.divIcon({
    className: 'car-marker',
    html: '🚗',
    iconSize: [28, 28],
    iconAnchor: [14, 14]
  });

  activeRide = signal<ActiveRideResponse | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  actionInProgress = signal(false);

  ngOnInit() {
    this.loadActiveRide();
  }

  ngOnDestroy() {
    this.stopSimulation();
    this.destroyMap();
  }

  loadActiveRide() {
    this.loading.set(true);
    this.error.set(null);

    this.rideService.getActiveRideForDriver().subscribe({
      next: (ride) => {
        this.activeRide.set(ride);
        this.refreshTracking();
        this.loading.set(false);
      },
      error: (err) => {
        if (err.status === 204) {
          // No active ride
          this.activeRide.set(null);
          this.refreshTracking();
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

  private refreshTracking() {
    const ride = this.activeRide();
    if (!ride || ride.status !== 'IN_PROGRESS') {
      this.stopSimulation();
      this.destroyMap();
      return;
    }

    this.routePoints = this.parseRouteCoordinates(ride.routeCoordinates);
    if (this.routePoints.length === 0) {
      this.stopSimulation();
      this.destroyMap();
      return;
    }

    setTimeout(() => this.initializeMap(ride), 0);
  }

  private parseRouteCoordinates(routeCoordinates?: string | number[][]): L.LatLngTuple[] {
    if (!routeCoordinates) {
      return [];
    }
    const raw = Array.isArray(routeCoordinates)
      ? routeCoordinates
      : this.safeParseCoordinates(routeCoordinates);
    if (!Array.isArray(raw)) {
      return [];
    }
    return raw
      .map((pair) => {
        if (!Array.isArray(pair) || pair.length < 2) {
          return null;
        }
        const lat = Number(pair[0]);
        const lng = Number(pair[1]);
        if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
          return null;
        }
        return [lat, lng] as L.LatLngTuple;
      })
      .filter((point): point is L.LatLngTuple => point !== null);
  }

  private safeParseCoordinates(raw: string): number[][] | null {
    try {
      const parsed = JSON.parse(raw);
      return Array.isArray(parsed) ? parsed : null;
    } catch {
      return null;
    }
  }

  private initializeMap(ride: ActiveRideResponse) {
    const container = document.getElementById('driver-map');
    if (!container || this.routePoints.length === 0) {
      return;
    }

    this.destroyMap();

    this.map = L.map(container, {
      zoomControl: true,
      attributionControl: false
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19
    }).addTo(this.map);

    this.routePolyline = L.polyline(this.routePoints, {
      color: '#22c55e',
      weight: 4
    }).addTo(this.map);

    this.carMarker = L.marker(this.routePoints[0], {
      icon: this.carIcon
    }).addTo(this.map);

    this.map.fitBounds(this.routePolyline.getBounds(), { padding: [20, 20] });
    this.startSimulation(ride.rideId);
  }

  private startSimulation(rideId: number) {
    this.stopSimulation();
    this.currentRouteIndex = 0;
    let previousPoint = this.routePoints[0];
    this.sendLocationUpdate(rideId, previousPoint, null);

    this.simulationTimer = setInterval(() => {
      if (!this.carMarker || this.routePoints.length === 0) {
        this.stopSimulation();
        return;
      }

      if (this.currentRouteIndex >= this.routePoints.length - 1) {
        this.stopSimulation();
        return;
      }

      const nextIndex = this.currentRouteIndex + 1;
      const nextPoint = this.routePoints[nextIndex];
      const heading = this.calculateHeading(previousPoint, nextPoint);

      this.currentRouteIndex = nextIndex;
      previousPoint = nextPoint;

      this.carMarker.setLatLng(nextPoint);
      this.map?.panTo(nextPoint, { animate: true, duration: 0.7 });
      this.sendLocationUpdate(rideId, nextPoint, heading);
    }, 1500);
  }

  private sendLocationUpdate(rideId: number, point: L.LatLngTuple, heading: number | null) {
    this.rideService.updateRideLocation(rideId, {
      latitude: point[0],
      longitude: point[1],
      heading,
      speed: null,
      recordedAt: new Date().toISOString()
    }).subscribe({
      error: (err) => console.error('Failed to update ride location:', err)
    });
  }

  private calculateHeading(from: L.LatLngTuple, to: L.LatLngTuple): number {
    const lat1 = (from[0] * Math.PI) / 180;
    const lon1 = (from[1] * Math.PI) / 180;
    const lat2 = (to[0] * Math.PI) / 180;
    const lon2 = (to[1] * Math.PI) / 180;
    const deltaLon = lon2 - lon1;
    const y = Math.sin(deltaLon) * Math.cos(lat2);
    const x =
      Math.cos(lat1) * Math.sin(lat2) -
      Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
    const bearing = Math.atan2(y, x);
    return (bearing * 180) / Math.PI;
  }

  private stopSimulation() {
    if (this.simulationTimer) {
      clearInterval(this.simulationTimer);
      this.simulationTimer = null;
    }
  }

  private destroyMap() {
    if (this.map) {
      this.map.remove();
      this.map = null;
    }
    this.carMarker = null;
    this.routePolyline = null;
  }
}
