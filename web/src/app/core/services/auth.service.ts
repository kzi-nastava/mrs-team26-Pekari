import { Injectable, signal, inject } from '@angular/core';
import { User } from '../models/user.model';
import { Observable, of, throwError } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { map, switchMap, catchError } from 'rxjs/operators';
import { EnvironmentService } from './environment.service';

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
  private env = inject(EnvironmentService);
  private currentUserSignal = signal<User | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();

  login(credentials: { email: string; password: string }): Observable<User> {
    return this.http
      .post<{ id?: string; userId?: string; email: string; role: string }>(`${this.env.getApiUrl()}/auth/login`, credentials, {
        withCredentials: true // Enable cookies to be sent/received
      })
      .pipe(
        map((resp) => {
          const role = this.normalizeRole(resp.role);
          if (!role) {
            throw new Error('Unsupported role');
          }

          const user: User = {
            id: resp.id || resp.userId || resp.email,
            email: resp.email,
            username: resp.email,
            role
          };
          this.currentUserSignal.set(user);
          return user;
        }),
        catchError(err => {
          const errorMessage = err.error?.message || 'Login failed';
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  private normalizeRole(role: string | null | undefined): User['role'] | null {
    const r = (role || '').toLowerCase();
    if (r === 'admin' || r === 'passenger' || r === 'driver') {
      return r;
    }
    return null;
  }

  register(userData: any): Observable<{ message: string; email: string }> {
    const formData = new FormData();
    formData.append('email', userData.email);
    // Auto-generate username from email since we don't have it in the form
    const autoUsername = userData.email.split('@')[0];
    formData.append('username', autoUsername);
    formData.append('password', userData.password);
    formData.append('firstName', userData.firstName);
    formData.append('lastName', userData.lastName);
    formData.append('address', userData.address);
    // Ensure phone number format matches backend requirement (digits only, optional +)
    // Remove spaces/dashes if any, keep +
    const cleanPhone = userData.phoneNumber.replace(/[^0-9+]/g, '');
    formData.append('phoneNumber', cleanPhone);

    // We are not sending profileImage for now

    return this.http.post<{ message: string; email: string }>(`${this.env.getApiUrl()}/auth/register/user`, formData)
      .pipe(
        catchError(err => {
          const errorMessage = err.error?.message || 'Registration failed';
          return throwError(() => new Error(errorMessage));
        })
      );
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${this.env.getApiUrl()}/auth/logout`, {}, {
      withCredentials: true // Send cookies to backend
    }).pipe(
      map(() => {
        this.currentUserSignal.set(null);
      }),
      catchError(err => {
        // Even if logout fails, clear the user locally
        this.currentUserSignal.set(null);
        return of(void 0);
      })
    );
  }

  isAuthenticated(): boolean {
    return this.currentUserSignal() !== null;
  }

  forgotPassword(email: string): Observable<void> {
    // TODO: Hook to real backend endpoint when implemented
    return this.http.post<void>(`${this.env.getApiUrl()}/auth/reset-password`, { email });
  }

  activate(token: string): Observable<{ message: string }> {
    return this.http.get<{ message: string }>(`${this.env.getApiUrl()}/auth/activate`, {
      params: { token }
    }).pipe(
      catchError(err => {
        const errorMessage = err.error?.message || 'Activation failed';
        return throwError(() => new Error(errorMessage));
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
      `${this.env.getApiUrl()}/auth/register/driver`,
      formData
    ).pipe(
      catchError(err => {
        const errorMessage = err.error?.message || 'Failed to register driver';
        return throwError(() => new Error(errorMessage));
      })
    );
  }
}
