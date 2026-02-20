import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { RideApiService, FavoriteRoute, CreateFavoriteRouteRequest, LocationPoint, PassengerRideHistoryResponse, PassengerRideDetailResponse } from '../../../core/services/ride-api.service';
import { PassengerRideDetailModalComponent } from './passenger-ride-detail-modal.component';

@Component({
  selector: 'app-passenger-history',
  standalone: true,
  imports: [CommonModule, FormsModule, PassengerRideDetailModalComponent],
  templateUrl: './passenger-history.component.html',
  styleUrls: ['./passenger-history.component.css']
})
export class PassengerHistoryComponent implements OnInit {
  private rides = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);
  private router = inject(Router);

  ridesList: any[] = [];
  favoriteRouteIds: Set<number> = new Set();
  favoriteRoutes: FavoriteRoute[] = [];
  loading = false;
  error?: string;

  // Modal state
  selectedRideId?: number;
  showModal = false;

  // Rating state
  showRatingModal = false;
  vehicleRating = 0;
  driverRating = 0;
  ratingComment = '';
  ratingSubmitting = false;
  ratingSuccess?: string;
  completedRideId?: number;

  startDate: string = '2024-01-01';
  endDate: string = new Date().toISOString().split('T')[0];

  // Sorting state
  sortField: string = 'date';
  sortDirection: 'asc' | 'desc' = 'desc';

  sortOptions = [
    { value: 'date', label: 'Date' },
    { value: 'price', label: 'Price' },
    { value: 'distance', label: 'Distance' },
    { value: 'vehicleType', label: 'Vehicle Type' },
    { value: 'status', label: 'Status' },
    { value: 'pickup', label: 'Pickup Address' },
    { value: 'dropoff', label: 'Dropoff Address' }
  ];

  ngOnInit(): void {
    this.loadRides();
    this.loadFavoriteRoutes();
  }

  loadRides() {
    this.loading = true;
    this.error = undefined;

    const filter = {
      startDate: this.startDate + 'T00:00:00',
      endDate: this.endDate + 'T23:59:59'
    };

    this.rides.getPassengerRideHistory(filter).subscribe({
      next: (response) => {
        this.ridesList = response.content.map(ride => this.mapRideForDisplay(ride));
        this.sortRides();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = 'Failed to load ride history';
        this.loading = false;
        console.error(err);
        this.cdr.detectChanges();
      }
    });
  }

  private mapRideForDisplay(ride: PassengerRideHistoryResponse): any {
    const startTime = ride.startTime ? new Date(ride.startTime) : null;
    const endTime = ride.endTime ? new Date(ride.endTime) : null;

    let duration = '';
    if (startTime && endTime) {
      const diffMs = endTime.getTime() - startTime.getTime();
      const diffMins = Math.round(diffMs / 60000);
      duration = ` (${diffMins} min)`;
    }

    const timeRange = `${startTime ? this.formatTime(startTime) : '??'} - ${endTime ? this.formatTime(endTime) : '??'}${duration}`;

    const statuses: string[] = [];
    if (ride.status === 'COMPLETED') statuses.push('completed');
    if (ride.panicActivated) statuses.push('panic');
    if (ride.status === 'CANCELLED') {
      if (ride.cancelledBy === 'driver') statuses.push('cancelled-driver');
      else statuses.push('cancelled-passenger');
    }

    // Check if ride is ratable (completed < 3 days ago)
    let isRatable = false;
    if (ride.status === 'COMPLETED' && endTime) {
      const now = new Date();
      const diffMs = now.getTime() - endTime.getTime();
      const diffDays = diffMs / (1000 * 60 * 60 * 24);
      isRatable = diffDays < 3;
    }

    return {
      id: ride.id,
      date: startTime ? this.formatDate(startTime) : 'Unknown date',
      time: timeRange,
      status: statuses,
      isRatable: isRatable,
      locations: [
        { type: 'start', label: 'Start', address: ride.pickup?.address || ride.pickupLocation, latitude: ride.pickup?.latitude, longitude: ride.pickup?.longitude },
        { type: 'end', label: 'Finish', address: ride.dropoff?.address || ride.dropoffLocation, latitude: ride.dropoff?.latitude, longitude: ride.dropoff?.longitude }
      ],
      stops: ride.stops?.map(s => ({
        address: s.address,
        latitude: s.latitude,
        longitude: s.longitude
      })) || [],
      distance: ride.distanceKm ? `${ride.distanceKm.toFixed(1)} km` : '',
      price: `${ride.price.toLocaleString()} RSD`,
      vehicleType: ride.vehicleType,
      babyTransport: ride.babyTransport,
      petTransport: ride.petTransport,
      rawRide: ride
    };
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false });
  }

  private formatDate(date: Date): string {
    return date.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' });
  }

  applyFilter() {
    this.loadRides();
  }

  resetFilter() {
    this.startDate = '2024-01-01';
    this.endDate = new Date().toISOString().split('T')[0];
    this.sortField = 'date';
    this.sortDirection = 'desc';
    this.loadRides();
  }

  onSortChange() {
    this.sortRides();
  }

  toggleSortDirection() {
    this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    this.sortRides();
  }

  private sortRides() {
    this.ridesList.sort((a, b) => {
      let comparison = 0;

      switch (this.sortField) {
        case 'date':
          const dateA = a.rawRide.startTime ? new Date(a.rawRide.startTime).getTime() : 0;
          const dateB = b.rawRide.startTime ? new Date(b.rawRide.startTime).getTime() : 0;
          comparison = dateA - dateB;
          break;
        case 'price':
          comparison = (a.rawRide.price || 0) - (b.rawRide.price || 0);
          break;
        case 'distance':
          comparison = (a.rawRide.distanceKm || 0) - (b.rawRide.distanceKm || 0);
          break;
        case 'vehicleType':
          comparison = (a.vehicleType || '').localeCompare(b.vehicleType || '');
          break;
        case 'status':
          comparison = (a.rawRide.status || '').localeCompare(b.rawRide.status || '');
          break;
        case 'pickup':
          const pickupA = a.locations[0]?.address || '';
          const pickupB = b.locations[0]?.address || '';
          comparison = pickupA.localeCompare(pickupB);
          break;
        case 'dropoff':
          const dropoffA = a.locations[a.locations.length - 1]?.address || '';
          const dropoffB = b.locations[b.locations.length - 1]?.address || '';
          comparison = dropoffA.localeCompare(dropoffB);
          break;
        default:
          comparison = 0;
      }

      return this.sortDirection === 'asc' ? comparison : -comparison;
    });
    this.cdr.detectChanges();
  }

  loadFavoriteRoutes() {
    this.rides.getFavoriteRoutes().subscribe({
      next: (routes) => {
        this.favoriteRoutes = routes;
        this.favoriteRouteIds = new Set(routes.map(r => r.id));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.debug('Failed to load favorite routes:', err);
      }
    });
  }

  toggleFavorite(ride: any) {
    const pickupAddress = ride.locations[0]?.address;
    const dropoffAddress = ride.locations[ride.locations.length - 1]?.address;

    // Find the favorite route that matches this ride
    const favoriteRoute = this.favoriteRoutes.find(r =>
      r.pickup?.address === pickupAddress &&
      r.dropoff?.address === dropoffAddress
    );

    if (favoriteRoute) {
      // Delete existing favorite
      this.rides.deleteFavoriteRoute(favoriteRoute.id).subscribe({
        next: () => {
          this.favoriteRouteIds.delete(favoriteRoute.id);
          this.favoriteRoutes = this.favoriteRoutes.filter(r => r.id !== favoriteRoute.id);
          delete ride.favoriteRouteId;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = 'Failed to remove from favorites';
          console.error(err);
          this.cdr.detectChanges();
        }
      });
    } else {
      // Create favorite route from ride data
      const pickup = ride.locations[0];
      const dropoff = ride.locations[ride.locations.length - 1];

      if (!pickup.latitude || !dropoff.latitude) {
        this.error = 'Cannot add to favorites: missing location coordinates';
        this.cdr.detectChanges();
        return;
      }

      const request: CreateFavoriteRouteRequest = {
        name: `${pickup.address} → ${dropoff.address}`,
        pickup: {
          address: pickup.address,
          latitude: pickup.latitude,
          longitude: pickup.longitude
        },
        stops: ride.stops?.map((stop: any) => ({
          address: stop.address,
          latitude: stop.latitude,
          longitude: stop.longitude
        })),
        dropoff: {
          address: dropoff.address,
          latitude: dropoff.latitude,
          longitude: dropoff.longitude
        },
        vehicleType: ride.vehicleType,
        babyTransport: ride.babyTransport,
        petTransport: ride.petTransport
      };

      this.loading = true;
      this.rides.createFavoriteRoute(request).subscribe({
        next: (createdRoute) => {
          this.favoriteRouteIds.add(createdRoute.id);
          this.favoriteRoutes.push(createdRoute);
          // Also mark the ride as favorite using a temporary ID
          ride.favoriteRouteId = createdRoute.id;
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error = 'Failed to add to favorites';
          this.loading = false;
          console.error(err);
          this.cdr.detectChanges();
        }
      });
    }
  }

  isFavorite(ride: any): boolean {
    // Check if there's a favorite route that matches this ride's pickup and dropoff
    const pickupAddress = ride.locations[0]?.address;
    const dropoffAddress = ride.locations[ride.locations.length - 1]?.address;

    if (!pickupAddress || !dropoffAddress) {
      return false;
    }

    return this.favoriteRoutes.some(favRoute =>
      favRoute.pickup?.address === pickupAddress &&
      favRoute.dropoff?.address === dropoffAddress
    ) || ride.favoriteRouteId !== undefined;
  }

  onRideClick(ride: any) {
    this.selectedRideId = ride.id;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.selectedRideId = undefined;
  }

  openRatingModal(ride: any) {
    this.completedRideId = ride.id;
    this.showRatingModal = true;
    this.vehicleRating = 0;
    this.driverRating = 0;
    this.ratingComment = '';
    this.ratingSuccess = undefined;
    this.error = undefined;
    this.cdr.detectChanges();
  }

  setVehicleRating(rating: number): void {
    this.vehicleRating = rating;
  }

  setDriverRating(rating: number): void {
    this.driverRating = rating;
  }

  submitRating(): void {
    if (!this.completedRideId || this.vehicleRating === 0 || this.driverRating === 0) {
      this.error = 'Please provide both vehicle and driver ratings';
      return;
    }

    this.ratingSubmitting = true;
    this.error = undefined;

    this.rides.rateRide(this.completedRideId, {
      vehicleRating: this.vehicleRating,
      driverRating: this.driverRating,
      comment: this.ratingComment.trim() || undefined
    }).subscribe({
      next: (response) => {
        this.ratingSubmitting = false;
        this.ratingSuccess = response.message || 'Rating submitted successfully!';
        setTimeout(() => {
          this.closeRatingModal();
        }, 2000);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.ratingSubmitting = false;
        this.error = err?.error?.message || 'Failed to submit rating';
        this.cdr.detectChanges();
      }
    });
  }

  closeRatingModal(): void {
    this.showRatingModal = false;
    this.vehicleRating = 0;
    this.driverRating = 0;
    this.ratingComment = '';
    this.ratingSuccess = undefined;
    this.completedRideId = undefined;
    this.cdr.detectChanges();
  }

  skipRating(): void {
    this.closeRatingModal();
  }

  onOrderSameRide(rideDetail: PassengerRideDetailResponse): void {
    this.closeModal();
    this.router.navigate(['/passenger-home'], {
      state: { rideConfig: rideDetail }
    });
  }
}
