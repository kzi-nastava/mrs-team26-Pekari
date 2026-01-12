import { Injectable, signal, inject } from '@angular/core';
import { User } from '../models/user.model';
import { Observable, map, of } from 'rxjs';
import { HttpClient } from '@angular/common/http';
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
    // TODO: Hook to real multipart register endpoint (/auth/register/user)
    const newUser: User = {
      id: Math.random().toString(36).substring(7),
      email: userData.email,
      username: userData.username || `${userData.firstName}${userData.lastName}`.toLowerCase(),
      firstName: userData.firstName,
      lastName: userData.lastName,
      role: 'passenger'
    };
    return of(newUser);
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
}
