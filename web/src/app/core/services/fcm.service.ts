import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { EnvironmentService } from './environment.service';
import { environment } from '../../../environments/environment';

// Lazy import to avoid bundling if not configured
let messaging: any = null;
let initializeApp: any = null;
let getMessaging: any = null;
let getToken: any = null;
let onMessage: any = null;

@Injectable({ providedIn: 'root' })
export class FcmService {
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private initialized = false;

  async tryInitAndRegister(): Promise<void> {
    if (this.initialized) return;
    const cfg = (environment as any).firebase as any;
    if (!cfg || !cfg.apiKey || !cfg.messagingSenderId || !cfg.appId) {
      // No Firebase web config, skip silently
      return;
    }

    try {
      // Dynamic import Firebase only when needed
      const firebaseApp = await import('firebase/app');
      const firebaseMessaging = await import('firebase/messaging');
      initializeApp = firebaseApp.initializeApp;
      messaging = firebaseMessaging;
      getMessaging = firebaseMessaging.getMessaging;
      getToken = firebaseMessaging.getToken;
      onMessage = firebaseMessaging.onMessage;

      const app = initializeApp({
        apiKey: cfg.apiKey,
        authDomain: cfg.authDomain,
        projectId: cfg.projectId,
        messagingSenderId: cfg.messagingSenderId,
        appId: cfg.appId
      });

      const msg = getMessaging(app);

      // Ask notification permission if not already granted
      const permission = await Notification.requestPermission();
      if (permission !== 'granted') {
        console.warn('[FCM] Notifications permission not granted');
        this.initialized = true; // Do not keep retrying
        return;
      }

      // Retrieve browser token using VAPID key if provided
      const token = await getToken(msg, cfg.vapidKey ? { vapidKey: cfg.vapidKey } : undefined);
      if (token) {
        await this.registerToken(token);
        // Optionally listen for foreground messages
        try {
          onMessage(msg, (payload: any) => {
            // Best-effort foreground toast using native Notification API
            const title = payload?.notification?.title || 'Notification';
            const body = payload?.notification?.body || '';
            if (Notification.permission === 'granted') {
              new Notification(title, { body });
            }
          });
        } catch {
          // ignore
        }
      } else {
        console.warn('[FCM] Failed to acquire browser token');
      }

      this.initialized = true;
    } catch (e) {
      console.warn('[FCM] Initialization failed:', (e as any)?.message || e);
      this.initialized = true; // avoid loops
    }
  }

  private async registerToken(token: string): Promise<void> {
    const api = this.env.getApiUrl();
    await this.http.post(`${api}/notifications/register-token`, { token }).toPromise();
  }
}
