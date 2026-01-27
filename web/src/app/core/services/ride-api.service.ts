import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';

export interface LocationPoint {
  address: string;
  latitude: number;
  longitude: number;
}

export interface EstimateRideRequest {
  pickup: LocationPoint;
  stops?: LocationPoint[];
  dropoff: LocationPoint;
  vehicleType: string;
}

export interface RideEstimateResponse {
  estimatedPrice: string;
  estimatedDurationMinutes: number;
  distanceKm: number;
  vehicleType: string;
  routePoints?: LocationPoint[];
}

export interface OrderRideRequest {
  pickup: LocationPoint;
  stops?: LocationPoint[];
  dropoff: LocationPoint;
  passengerEmails?: string[];
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  scheduledAt?: string | null;
}

export interface OrderRideResponse {
  rideId: number;
  status: string;
  message: string;
  estimatedPrice: number | string;
  scheduledAt?: string | null;
  assignedDriverEmail?: string | null;
}

export interface PassengerInfo {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
}

export interface DriverInfo {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  vehicleType: string;
  licensePlate: string;
}

export interface ActiveRideResponse {
  rideId: number;
  status: string;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  scheduledAt?: string | null;
  estimatedPrice: number;
  distanceKm: number;
  estimatedDurationMinutes: number;
  startedAt?: string | null;
  pickup: LocationPoint;
  dropoff: LocationPoint;
  stops?: LocationPoint[];
  passengers: PassengerInfo[];
  driver?: DriverInfo;
}

export interface MessageResponse {
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class RideApiService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);

  estimateRide(request: EstimateRideRequest) {
    return this.http.post<RideEstimateResponse>(`${this.env.getApiUrl()}/rides/estimate`, request);
  }

  orderRide(request: OrderRideRequest) {
    const token = localStorage.getItem('auth_token');
    if (token) {
      return this.http.post<OrderRideResponse>(
        `${this.env.getApiUrl()}/rides/order`,
        request,
        { headers: { 'Authorization': `Bearer ${token}` } }
      );
    }
    return this.http.post<OrderRideResponse>(`${this.env.getApiUrl()}/rides/order`, request);
  }

  // Driver methods
  getActiveRideForDriver() {
    return this.http.get<ActiveRideResponse>(`${this.env.getApiUrl()}/rides/active/driver`);
  }

  getActiveRideForPassenger() {
    return this.http.get<ActiveRideResponse>(`${this.env.getApiUrl()}/rides/active/passenger`);
  }

  startRide(rideId: number) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/start`, {});
  }

  completeRide(rideId: number) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/stop`, {});
  }

  cancelRide(rideId: number, reason: string) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/cancel`, { reason });
  }
}
