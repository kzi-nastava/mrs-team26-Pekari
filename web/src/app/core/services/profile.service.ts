import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, map, throwError, catchError } from 'rxjs';
import { ProfileData, DriverInfo, ProfileUpdateRequest, ApprovalRequest, PasswordChangeRequest } from '../models/profile.model';
import { EnvironmentService } from './environment.service';
import { AuthService } from './auth.service';

// Backend response types
interface DriverProfileResponse {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  profilePicture: string | null;
  createdAt: string;
  updatedAt: string;
  licenseNumber: string;
  licenseExpiry: string;
  vehicleRegistration: string;
  vehicleModel: string;
  vehicleType: string;
  licensePlate: string;
  numberOfSeats: number;
  babyFriendly: boolean;
  petFriendly: boolean;
  averageRating: number;
  totalRides: number;
  isActive: boolean;
}

interface PassengerProfileResponse {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  profilePicture: string | null;
  createdAt: string;
  updatedAt: string;
  totalRides: number;
  averageRating: number;
}

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private authService = inject(AuthService);

  private get apiUrl(): string {
    return `${this.env.getApiUrl()}/profile`;
  }

  /**
   * Get current user's profile based on their role
   */
  getProfile(): Observable<ProfileData> {
    const currentUser = this.authService.currentUser();
    const role = currentUser?.role;

    if (role === 'driver') {
      return this.http.get<DriverProfileResponse>(`${this.apiUrl}/driver`).pipe(
        map(response => this.mapDriverResponseToProfileData(response))
      );
    } else if (role === 'admin') {
      return this.http.get<PassengerProfileResponse>(`${this.apiUrl}/admin`).pipe(
        map(response => this.mapPassengerResponseToProfileData(response, 'admin'))
      );
    } else {
      // Default to passenger endpoint
      return this.http.get<PassengerProfileResponse>(`${this.apiUrl}/passenger`).pipe(
        map(response => this.mapPassengerResponseToProfileData(response, 'passenger'))
      );
    }
  }

  private mapDriverResponseToProfileData(response: DriverProfileResponse): ProfileData {
    return {
      id: response.id,
      email: response.email,
      username: response.username,
      firstName: response.firstName,
      lastName: response.lastName,
      phoneNumber: response.phoneNumber,
      address: response.address,
      role: 'driver',
      profilePicture: response.profilePicture || undefined,
      createdAt: new Date(response.createdAt),
      updatedAt: new Date(response.updatedAt)
    };
  }

  private mapPassengerResponseToProfileData(response: PassengerProfileResponse, role: 'passenger' | 'admin' = 'passenger'): ProfileData {
    return {
      id: response.id,
      email: response.email,
      username: response.username,
      firstName: response.firstName,
      lastName: response.lastName,
      phoneNumber: response.phoneNumber,
      address: response.address,
      role: role,
      profilePicture: response.profilePicture || undefined,
      createdAt: new Date(response.createdAt),
      updatedAt: new Date(response.updatedAt)
    };
  }

  /**
   * Get driver-specific information (hours active, vehicle)
   * Fetches from /profile/driver - same endpoint as getProfile() for drivers
   */
  getDriverInfo(): Observable<DriverInfo> {
    return this.http.get<DriverProfileResponse>(`${this.apiUrl}/driver`).pipe(
      map(response => this.mapDriverProfileToDriverInfo(response))
    );
  }

  private mapDriverProfileToDriverInfo(response: DriverProfileResponse): DriverInfo {
    const vehicleModel = response.vehicleModel?.trim() || '';
    const vehicleType = response.vehicleType?.trim() || '';
    const parts = vehicleModel.split(/\s+/).filter(Boolean);
    const make = parts.length > 1 ? parts[0] : '';
    const model =
      parts.length > 0
        ? parts.length > 1
          ? parts.slice(1).join(' ')
          : parts[0]
        : vehicleType || '';

    return {
      hoursActiveLast24h: 0, // Backend does not expose this yet
      vehicle: {
        id: response.id,
        make,
        model,
        year: 0, // Backend does not store year separately
        licensePlate: response.licensePlate || '',
        vin: response.vehicleRegistration || ''
      }
    };
  }

  /**
   * Update user profile (creates approval request for drivers)
   */
  updateProfile(updateData: ProfileUpdateRequest): Observable<{ success: boolean; message: string }> {
    const currentUser = this.authService.currentUser();
    const role = currentUser?.role;

    let endpoint: string;
    if (role === 'driver') {
      endpoint = `${this.apiUrl}/driver`;
    } else if (role === 'admin') {
      endpoint = `${this.apiUrl}/admin`;
    } else {
      endpoint = `${this.apiUrl}/passenger`;
    }

    return this.http.put<{ message: string }>(endpoint, updateData).pipe(
      map(response => ({ success: true, message: response.message }))
    );
  }

  /**
   * Get pending approval requests for driver (admin view)
   */
  getApprovalRequests(): Observable<ApprovalRequest[]> {
    // Replace with actual HTTP call
    return of([]);
  }

  /**
   * Approve profile update request (admin only)
   */
  approveProfileUpdate(requestId: string): Observable<{ success: boolean }> {
    // Replace with actual HTTP call
    return of({ success: true });
  }

  /**
   * Reject profile update request (admin only)
   */
  rejectProfileUpdate(requestId: string, reason: string): Observable<{ success: boolean }> {
    // Replace with actual HTTP call
    return of({ success: true });
  }

  /**
   * Change password
   */
  changePassword(passwordData: PasswordChangeRequest): Observable<{ success: boolean; message: string }> {
    return this.http.post<{ message: string }>(`${this.apiUrl}/change-password`, passwordData).pipe(
      map(response => ({ success: true, message: response.message })),
      catchError(err => {
        const errorMessage = err.error?.message || 'Password change failed';
        return throwError(() => new Error(errorMessage));
      })
    );
  }

  /**
   * Upload profile picture
   */
  uploadProfilePicture(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    // Replace with actual HTTP call
    return of({ url: URL.createObjectURL(file) });
  }

  /**
   * Get default profile picture
   */
  getDefaultProfilePicture(): string {
    return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCIgdmlld0JveD0iMCAwIDEwMCAxMDAiIGZpbGw9Im5vbmUiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+CjxyZWN0IHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiBmaWxsPSIjMzMzMzMzIi8+CjxjaXJjbGUgY3g9IjUwIiBjeT0iMzUiIHI9IjE1IiBmaWxsPSIjODg4ODg4Ii8+CjxwYXRoIGQ9Ik0gMjAgNzAgQSAzMCAzMCAwIDAgMSA4MCA3MCIgZmlsbD0iIzg4ODg4OCIvPgo8L3N2Zz4=';
  }
}
