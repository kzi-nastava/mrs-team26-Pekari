import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EnvironmentService } from './environment.service';

export interface UserListItem {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  blocked: boolean;
  blockedNote: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);

  private get adminUrl(): string {
    return `${this.env.getApiUrl()}/admin`;
  }

  getDrivers(): Observable<UserListItem[]> {
    return this.http.get<UserListItem[]>(`${this.adminUrl}/drivers`);
  }

  getPassengers(): Observable<UserListItem[]> {
    return this.http.get<UserListItem[]>(`${this.adminUrl}/passengers`);
  }

  setUserBlock(userId: string, blocked: boolean, note?: string | null): Observable<{ message: string }> {
    return this.http.patch<{ message: string }>(`${this.adminUrl}/users/${userId}`, {
      blocked,
      blockedNote: blocked ? (note ?? null) : null
    });
  }
}
