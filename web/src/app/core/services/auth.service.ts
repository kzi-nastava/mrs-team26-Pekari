import { Injectable, signal, inject } from '@angular/core';
import { User } from '../models/user.model';
import { Observable, of, throwError, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { map, switchMap, catchError } from 'rxjs/operators';
import { environment } from '../../../environments/environment';

export interface RegisterDriverData {
  firstName: string;
  lastName: string;
  email: string;
  address: string;
  phoneNumber: string;
  vehicleModel: string;
  vehicleType: string;
  licensePlate: string;
  numberOfSeats: number;
  babyFriendly: boolean;
  petFriendly: boolean;
}

export interface RegisterDriverResponse {
  message: string;
  email: string;
  status: string;
}

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private currentUserSignal = signal<User | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();

  constructor() {
    this.checkSession();
  }

  private checkSession(): void {
    // This will be replaced with a call to a "Who Am I" endpoint
    // to fetch the user based on the session cookie/token.
  }

  login(credentials: any): Observable<User> {
    // Simulated API call delay
    return timer(500).pipe(
      switchMap(() => {
        if (credentials.email === 'error@example.com') {
          return throwError(() => new Error('Invalid credentials'));
        }

        const mockUser: User = {
          id: '1',
          email: credentials.email || 'user@example.com',
          username: 'testuser',
          role: 'passenger'
        };

        this.currentUserSignal.set(mockUser);
        return of(mockUser);
      })
    );
  }

  register(userData: any): Observable<User> {
    // Simulated API call delay
    return timer(500).pipe(
      map(() => {
        const newUser: User = {
          id: Math.random().toString(36).substring(7),
          email: userData.email,
          username: userData.username || `${userData.firstName}${userData.lastName}`.toLowerCase(),
          firstName: userData.firstName,
          lastName: userData.lastName,
          role: 'passenger'
        };
        // No signal used, user has to activate account via email
        return newUser;
      })
    );
  }

  logout(): void {
    this.currentUserSignal.set(null);
  }

  isAuthenticated(): boolean {
    return this.currentUserSignal() !== null;
  }

  forgotPassword(email: string): Observable<void> {
    // Simulated API call delay
    return timer(500).pipe(
      map(() => {
        console.log(`Password reset instructions sent to: ${email}`);
        return;
      })
    );
  }

  registerDriver(driverData: Partial<RegisterDriverData>): Observable<RegisterDriverResponse> {
    const formData = new FormData();
    formData.append('email', driverData.email || '');
    formData.append('firstName', driverData.firstName || '');
    formData.append('lastName', driverData.lastName || '');
    formData.append('address', driverData.address || '');
    formData.append('phoneNumber', driverData.phoneNumber || '');

    // Vehicle data as nested object for multipart/form-data
    formData.append('vehicle.model', driverData.vehicleModel || '');
    formData.append('vehicle.type', driverData.vehicleType || 'STANDARD');
    formData.append('vehicle.licensePlate', driverData.licensePlate || '');
    formData.append('vehicle.numberOfSeats', String(driverData.numberOfSeats || 4));
    formData.append('vehicle.babyFriendly', String(driverData.babyFriendly || false));
    formData.append('vehicle.petFriendly', String(driverData.petFriendly || false));

    return this.http.post<RegisterDriverResponse>(
      `${environment.apiUrl}/v1/auth/register/driver`,
      formData
    ).pipe(
      catchError(err => {
        const errorMessage = err.error?.message || 'Failed to register driver';
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
