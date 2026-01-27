import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, FavoriteRoute, CreateFavoriteRouteRequest, LocationPoint } from '../../../core/services/ride-api.service';

@Component({
  selector: 'app-passenger-history',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './passenger-history.component.html',
  styleUrls: ['./passenger-history.component.css']
})
export class PassengerHistoryComponent implements OnInit {
  private rides = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);

  ridesList: any[] = [];
  favoriteRouteIds: Set<number> = new Set();
  loading = false;
  error?: string;

  ngOnInit(): void {
    this.loadRides();
    this.loadFavoriteRoutes();
  }

  loadRides() {
    // TODO: Replace with actual API call when backend is ready
    // For now, using mock data similar to driver history
    this.ridesList = [
      {
        id: 1,
        date: '15 December 2024',
        time: '14:30 - 15:15 (45 min)',
        status: ['completed'],
        locations: [
          { type: 'start', label: 'Start', address: 'Knez Mihailova 12, Beograd', latitude: 45.2671, longitude: 19.8335 },
          { type: 'end', label: 'Finish', address: 'Aerodrom Nikola Tesla, Beograd', latitude: 45.255, longitude: 19.845 }
        ],
        stops: [],
        distance: '18.5 km',
        price: '1,250 RSD',
        vehicleType: 'STANDARD',
        babyTransport: false,
        petTransport: false
      },
      {
        id: 2,
        date: '14 December 2024',
        time: '09:15 - 09:45 (30 min)',
        status: ['completed'],
        locations: [
          { type: 'start', label: 'Start', address: 'Slavija trg 1, Beograd', latitude: 45.26, longitude: 19.84 },
          { type: 'end', label: 'Finish', address: 'Ušće Shopping Center, Beograd', latitude: 45.25, longitude: 19.85 }
        ],
        stops: [
          { address: 'Trg slobode', latitude: 45.258, longitude: 19.842 }
        ],
        distance: '8.2 km',
        price: '650 RSD',
        vehicleType: 'VAN',
        babyTransport: true,
        petTransport: false
      }
    ];
  }

  loadFavoriteRoutes() {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      return;
    }

    this.rides.getFavoriteRoutes().subscribe({
      next: (routes) => {
        this.favoriteRouteIds = new Set(routes.map(r => r.id));
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.debug('Failed to load favorite routes:', err);
      }
    });
  }

  toggleFavorite(ride: any) {
    const isFavorite = this.favoriteRouteIds.has(ride.id);

    if (isFavorite) {
      // Find the favorite route ID and delete it
      this.rides.getFavoriteRoutes().subscribe({
        next: (routes) => {
          const favoriteRoute = routes.find(r => 
            r.pickup.address === ride.locations[0].address &&
            r.dropoff.address === ride.locations[ride.locations.length - 1].address
          );
          
          if (favoriteRoute) {
            this.rides.deleteFavoriteRoute(favoriteRoute.id).subscribe({
              next: () => {
                this.favoriteRouteIds.delete(ride.id);
                this.cdr.detectChanges();
              },
              error: (err) => {
                this.error = 'Failed to remove from favorites';
                console.error(err);
              }
            });
          }
        }
      });
    } else {
      // Create favorite route from ride data
      const pickup = ride.locations[0];
      const dropoff = ride.locations[ride.locations.length - 1];
      
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
        next: (favoriteRoute) => {
          this.favoriteRouteIds.add(favoriteRoute.id);
          // Also mark the ride as favorite using a temporary ID
          ride.favoriteRouteId = favoriteRoute.id;
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
    return this.favoriteRouteIds.has(ride.id) || ride.favoriteRouteId !== undefined;
  }
}
