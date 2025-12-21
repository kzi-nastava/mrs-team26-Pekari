import { Injectable, signal, inject } from '@angular/core';
import { User } from '../models/user.model';
import { Observable, of, throwError, timer } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { map, switchMap } from 'rxjs/operators';

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

        // Note: We don't set the user signal here anymore because
        // the user needs to activate their account first.
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
}
