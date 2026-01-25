import { Injectable, signal, inject } from '@angular/core';
import { User } from '../models/user.model';
import { Observable, map, of } from 'rxjs';
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
import { EnvironmentService } from './environment.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private currentUserSignal = signal<User | null>(null);

  readonly currentUser = this.currentUserSignal.asReadonly();

  constructor() {
    this.checkSession();
  }

  private checkSession(): void {
    const token = localStorage.getItem('auth_token');
    const email = localStorage.getItem('auth_email');
    const role = localStorage.getItem('auth_role');
    if (token && email && role) {
      const normalizedRole = this.normalizeRole(role);
      if (normalizedRole) {
        this.currentUserSignal.set({ id: 'me', email, username: email, role: normalizedRole });
      }
    }
  }

  login(credentials: { email: string; password: string }): Observable<User> {
    return this.http
      .post<{ token: string; email: string; role: string }>(`${this.env.getApiUrl()}/auth/login`, credentials)
      .pipe(
        map((resp) => {
          const role = this.normalizeRole(resp.role);
          if (!role) {
            throw new Error('Unsupported role');
          }
          localStorage.setItem('auth_token', resp.token);
          localStorage.setItem('auth_email', resp.email);
          localStorage.setItem('auth_role', role);

          const user: User = {
            id: 'me',
            email: resp.email,
            username: resp.email,
            role
          };
          this.currentUserSignal.set(user);
          return user;
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

  register(userData: any): Observable<User> {
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

    return this.http.post<any>(`${this.env.getApiUrl()}/auth/register/user`, formData)
      .pipe(
        map(resp => {
          // Construct a temporary User object from response + input data
          // Actual login will happen when they click the email link or login manually
          const newUser: User = {
            id: String(resp.userId),
            email: resp.email,
            username: autoUsername,
            firstName: userData.firstName,
            lastName: userData.lastName,
            role: 'passenger'
          };
          return newUser;
        })
      );
  }

  logout(): void {
    this.currentUserSignal.set(null);
    localStorage.removeItem('auth_token');
    localStorage.removeItem('auth_email');
    localStorage.removeItem('auth_role');
  }

  isAuthenticated(): boolean {
    return this.currentUserSignal() !== null;
  }

  forgotPassword(email: string): Observable<void> {
    // TODO: Hook to real backend endpoint when implemented
    return this.http.post<void>(`${this.env.getApiUrl()}/auth/reset-password`, { email });
  }

  activate(token: string): Observable<void> {
    return this.http.get<void>(`${this.env.getApiUrl()}/auth/activate`, {
      params: { token }
    });
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
