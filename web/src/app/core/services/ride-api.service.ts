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

export interface CancelRideRequest {
  reason: string;
}

export interface CancelRideResponse {
  message: string;
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
  routeCoordinates?: string | number[][];
  pickup: LocationPoint;
  dropoff: LocationPoint;
  stops?: LocationPoint[];
  passengers: PassengerInfo[];
  driver?: DriverInfo;
}

export interface MessageResponse {
  message: string;
}

export interface StopRideEarlyRequest {
  stopLocation: LocationPoint;
}

export interface RideLocationUpdateRequest {
  latitude: number;
  longitude: number;
  heading?: number | null;
  speed?: number | null;
  recordedAt?: string | null;
}

export interface FavoriteRoute {
  id: number;
  name?: string;
  pickup: LocationPoint;
  stops?: LocationPoint[];
  dropoff: LocationPoint;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
}

export interface CreateFavoriteRouteRequest {
  name?: string;
  pickup: LocationPoint;
  stops?: LocationPoint[];
  dropoff: LocationPoint;
  vehicleType?: string;
  babyTransport?: boolean;
  petTransport?: boolean;
}

export interface DriverRideHistoryResponse {
  id: number;
  startTime: string | null;
  endTime: string | null;
  pickupLocation: string;
  dropoffLocation: string;
  cancelled: boolean;
  cancelledBy: string | null;
  price: number;
  panicActivated: boolean;
  status: string;
  passengers: PassengerHistoryInfo[];
}

export interface PassengerHistoryInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface RideHistoryFilterRequest {
  startDate: string;
  endDate: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
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
    return this.http.post<OrderRideResponse>(`${this.env.getApiUrl()}/rides/order`, request);
  }

  cancelRide(rideId: number, reason: string) {
    return this.http.post<CancelRideResponse>(
      `${this.env.getApiUrl()}/rides/${rideId}/cancel`,
      { reason }
    );
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

  completeRide(rideId: number, stopLocation?: LocationPoint) {
    const body = stopLocation ? { stopLocation } : {};
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/stop`, body);
  }

  requestStopRide(rideId: number) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/request-stop`, {});
  }

  updateRideLocation(rideId: number, payload: RideLocationUpdateRequest) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/location`, payload);
  }

  // Favorite routes methods
  getFavoriteRoutes() {
    const token = localStorage.getItem('auth_token');
    return this.http.get<FavoriteRoute[]>(`${this.env.getApiUrl()}/profile/favourite-routes`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
  }

  createFavoriteRoute(request: CreateFavoriteRouteRequest) {
    const token = localStorage.getItem('auth_token');
    return this.http.post<FavoriteRoute>(`${this.env.getApiUrl()}/profile/favourite-routes`, request, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
  }

  deleteFavoriteRoute(id: number) {
    const token = localStorage.getItem('auth_token');
    return this.http.delete<MessageResponse>(`${this.env.getApiUrl()}/profile/favourite-routes/${id}`, {
      headers: token ? { 'Authorization': `Bearer ${token}` } : {}
    });
  }

  reportInconsistency(rideId: number, description: string) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/report-inconsistency`, {
      description
    });
  }

  getDriverRideHistory(startDate: string, endDate: string, page: number = 0, size: number = 20) {
    return this.http.get<PaginatedResponse<DriverRideHistoryResponse>>(
      `${this.env.getApiUrl()}/rides/history/driver?startDate=${startDate}&endDate=${endDate}&page=${page}&size=${size}`
    );
  }
}
