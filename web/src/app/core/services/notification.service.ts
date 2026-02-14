import { Injectable, inject, signal, effect } from '@angular/core';
import { WebSocketService } from './websocket.service';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

export interface UserNotification {
  rideId: number;
  status: string;
  message: string;
  read: boolean;
  timestamp: Date;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private ws = inject(WebSocketService);
  private auth = inject(AuthService);
  private router = inject(Router);

  notifications = signal<UserNotification[]>([]);
  unreadCount = signal(0);

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (user) {
        this.subscribeToNotifications(user.email);
      } else {
        this.notifications.set([]);
        this.unreadCount.set(0);
      }
    });
  }

  private subscribeToNotifications(email: string) {
    this.ws.subscribeTo<any>(`/topic/notifications/${email}`).subscribe(n => {
      const newNotif: UserNotification = { ...n, read: false, timestamp: new Date() };
      this.notifications.update(list => [newNotif, ...list]);
      this.unreadCount.update(c => c + 1);

      // Optional: Auto-hide toast if we were using one, but for now we just keep them in list
    });
  }

  markAllAsRead() {
    this.notifications.update(list => list.map(n => ({ ...n, read: true })));
    this.unreadCount.set(0);
  }

  clearNotifications() {
    this.notifications.set([]);
    this.unreadCount.set(0);
  }
}
