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
  panickedBy: string | null;
  status: string;
  passengers: PassengerHistoryInfo[];
}

export interface PassengerRideHistoryResponse {
  id: number;
  startTime: string | null;
  endTime: string | null;
  pickupLocation: string;
  dropoffLocation: string;
  pickup?: LocationPoint;
  dropoff?: LocationPoint;
  stops?: LocationPoint[];
  cancelled: boolean;
  cancelledBy: string | null;
  price: number;
  panicActivated: boolean;
  status: string;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  distanceKm: number;
  driver: DriverHistoryInfo | null;
}

export interface DriverHistoryInfo {
  id: number;
  firstName: string;
  lastName: string;
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

export interface RideRatingRequest {
  vehicleRating: number;
  driverRating: number;
  comment?: string;
}

// Admin ride history interfaces
export interface AdminRideHistoryResponse {
  id: number;
  status: string;
  createdAt: string | null;
  scheduledAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  pickupAddress: string;
  dropoffAddress: string;
  pickup: LocationPoint;
  dropoff: LocationPoint;
  stops: LocationPoint[];
  cancelled: boolean;
  cancelledBy: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  price: number;
  distanceKm: number;
  estimatedDurationMinutes: number;
  panicActivated: boolean;
  panickedBy: string | null;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  driver: AdminDriverBasicInfo | null;
  passengers: AdminPassengerBasicInfo[];
}

export interface AdminDriverBasicInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
}

export interface AdminPassengerBasicInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface AdminRideHistoryFilter {
  startDate?: string;
  endDate?: string;
}

// Ride stats interfaces
export interface RideStatsDayDto {
  date: string;
  rideCount: number;
  distanceKm: number;
  amount: number;
}

export interface RideStatsResponse {
  dailyData: RideStatsDayDto[];
  totalRides: number;
  totalDistanceKm: number;
  totalAmount: number;
  avgRidesPerDay: number;
  avgDistancePerDay: number;
  avgAmountPerDay: number;
}

export interface DriverBasicInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

export interface PassengerBasicInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
}

// Admin ride detail interfaces
export interface AdminRideDetailResponse {
  id: number;
  status: string;
  createdAt: string | null;
  scheduledAt: string | null;
  startedAt: string | null;
  completedAt: string | null;
  pickupAddress: string;
  dropoffAddress: string;
  pickup: LocationPoint;
  dropoff: LocationPoint;
  stops: LocationPoint[];
  routeCoordinates: string | null; // JSON string of route coordinates
  cancelled: boolean;
  cancelledBy: string | null;
  cancellationReason: string | null;
  cancelledAt: string | null;
  price: number;
  distanceKm: number;
  estimatedDurationMinutes: number;
  panicActivated: boolean;
  panickedBy: string | null;
  vehicleType: string;
  babyTransport: boolean;
  petTransport: boolean;
  driver: AdminDriverDetailInfo | null;
  passengers: AdminPassengerDetailInfo[];
  ratings: AdminRideRatingInfo[];
  inconsistencyReports: AdminInconsistencyReportInfo[];
}

export interface AdminDriverDetailInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  profilePicture: string | null;
  licenseNumber: string;
  vehicleModel: string;
  licensePlate: string;
  averageRating: number | null;
  totalRides: number;
}

export interface AdminPassengerDetailInfo {
  id: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  profilePicture: string | null;
  totalRides: number;
  averageRating: number | null;
}

export interface AdminRideRatingInfo {
  id: number;
  passengerId: number;
  passengerName: string;
  vehicleRating: number;
  driverRating: number;
  comment: string | null;
  ratedAt: string;
}

export interface AdminInconsistencyReportInfo {
  id: number;
  reportedByUserId: number;
  reportedByName: string;
  description: string;
  reportedAt: string;
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

  getPassengerRideHistory(filter: RideHistoryFilterRequest, page: number = 0, size: number = 20) {
    return this.http.post<PaginatedResponse<PassengerRideHistoryResponse>>(
      `${this.env.getApiUrl()}/rides/history/passenger?page=${page}&size=${size}`,
      filter
    );
  }

  // Panic mode methods
  activatePanic(rideId: number) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/panic`, {});
  }

  getActivePanicRides() {
    return this.http.get<DriverRideHistoryResponse[]>(`${this.env.getApiUrl()}/rides/panic/active`);
  }
  rateRide(rideId: number, rating: RideRatingRequest) {
    return this.http.post<MessageResponse>(`${this.env.getApiUrl()}/rides/${rideId}/rate`, rating);
  }

  // Admin methods
  getAdminRideHistory(filter: AdminRideHistoryFilter, page: number = 0, size: number = 20) {
    return this.http.post<PaginatedResponse<AdminRideHistoryResponse>>(
      `${this.env.getApiUrl()}/rides/history/admin/all?page=${page}&size=${size}`,
      filter
    );
  }

  getAdminRideDetail(rideId: number) {
    return this.http.get<AdminRideDetailResponse>(`${this.env.getApiUrl()}/rides/admin/${rideId}`);
  }

  // Ride stats methods
  getDriverRideStats(startDate: string, endDate: string) {
    return this.http.get<RideStatsResponse>(
      `${this.env.getApiUrl()}/rides/stats/driver?startDate=${startDate}&endDate=${endDate}`
    );
  }

  getPassengerRideStats(startDate: string, endDate: string) {
    return this.http.get<RideStatsResponse>(
      `${this.env.getApiUrl()}/rides/stats/passenger?startDate=${startDate}&endDate=${endDate}`
    );
  }

  getAdminRideStats(params: { startDate: string; endDate: string; scope: string; userId?: number }) {
    let url = `${this.env.getApiUrl()}/rides/stats/admin?startDate=${params.startDate}&endDate=${params.endDate}&scope=${params.scope}`;
    if (params.userId != null) {
      url += `&userId=${params.userId}`;
    }
    return this.http.get<RideStatsResponse>(url);
  }

  getAdminDrivers() {
    return this.http.get<DriverBasicInfo[]>(`${this.env.getApiUrl()}/admin/drivers`);
  }

  getAdminPassengers() {
    return this.http.get<PassengerBasicInfo[]>(`${this.env.getApiUrl()}/admin/passengers`);
  }
}
