import { Injectable, inject, signal, effect } from '@angular/core';
import { WebSocketService } from './websocket.service';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';
import { FcmService } from './fcm.service';

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
  private fcm = inject(FcmService);

  notifications = signal<UserNotification[]>([]);
  unreadCount = signal(0);

  constructor() {
    effect(() => {
      const user = this.auth.currentUser();
      if (user) {
        this.subscribeToNotifications(user.email);
        // Best-effort: initialize FCM (if configured) and register browser token
        this.fcm.tryInitAndRegister();
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

      // Also surface via native browser notification as a fallback (when permission granted)
      try {
        if (typeof Notification !== 'undefined' && Notification.permission === 'granted') {
          new Notification('BlackCar', { body: newNotif.message });
        }
      } catch {
        // ignore
      }
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
