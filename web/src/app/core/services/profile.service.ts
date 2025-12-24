import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { ProfileData, DriverInfo, ProfileUpdateRequest, ApprovalRequest, PasswordChangeRequest } from '../models/profile.model';

@Injectable({
  providedIn: 'root'
})
export class ProfileService {
  private http = inject(HttpClient);
  private apiUrl = 'http://localhost:8080/api/profile'; // Update with actual backend URL

  // Mock profile data for demo
  private mockProfile: ProfileData = {
    id: '1',
    email: 'john@example.com',
    username: 'johndoe',
    firstName: 'John',
    lastName: 'Doe',
    phoneNumber: '+381 64 000 000',
    address: '123 Main Street, City',
    role: 'driver',
    createdAt: new Date('2024-01-15'),
    updatedAt: new Date('2024-01-15')
  };

  private mockDriverInfo: DriverInfo = {
    hoursActiveLast24h: 5.33,
    vehicle: {
      id: 'v1',
      make: 'Tesla',
      model: 'Model 3',
      year: 2023,
      licensePlate: 'TS-123-AB',
      vin: '12345678901234567'
    }
  };

  /**
   * Get current user's profile
   */
  getProfile(): Observable<ProfileData> {
    // Replace with actual HTTP call
    return of(this.mockProfile);
  }

  /**
   * Get driver-specific information (hours active, vehicle)
   */
  getDriverInfo(): Observable<DriverInfo> {
    // Replace with actual HTTP call
    return of(this.mockDriverInfo);
  }

  /**
   * Update user profile (creates approval request for drivers)
   */
  updateProfile(updateData: ProfileUpdateRequest): Observable<{ success: boolean; message: string }> {
    // For drivers: creates approval request
    // For admin/passenger: updates immediately
    // Replace with actual HTTP call
    return of({ success: true, message: 'Profile update request sent for approval' });
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
    // Replace with actual HTTP call
    return of({ success: true, message: 'Password changed successfully' });
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
