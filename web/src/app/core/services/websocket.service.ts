import { Injectable, inject, OnDestroy } from '@angular/core';
import { Observable, Subject, BehaviorSubject, timer } from 'rxjs';
import { filter, takeUntil } from 'rxjs/operators';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';
import { EnvironmentService } from './environment.service';

export interface RideTrackingUpdate {
  rideId: number;
  vehicleLatitude: number;
  vehicleLongitude: number;
  estimatedTimeToDestinationMinutes?: number;
  distanceToDestinationKm?: number;
  status?: string;
  rideStatus?: string; // Added to handle COMPLETED status
  nextStopName?: string;
  nextStopEta?: number;
  updatedAt?: string;
  recordedAt?: string;
  vehicle?: {
    id: number;
    type: string;
    licensePlate: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class WebSocketService implements OnDestroy {
  private env = inject(EnvironmentService);
  private client: Client | null = null;
  private subscriptions = new Map<string, StompSubscription>();
  private messageSubjects = new Map<string, Subject<any>>();
  private connectionState = new BehaviorSubject<boolean>(false);
  private destroy$ = new Subject<void>();

  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;

  get isConnected$(): Observable<boolean> {
    return this.connectionState.asObservable();
  }

  get isConnected(): boolean {
    return this.connectionState.value;
  }

  connect(): void {
    if (this.client?.active) {
      return;
    }

    const apiUrl = this.env.getApiUrl();
    // Convert http://localhost:8080/api/v1 to ws://localhost:8080/ws
    const wsUrl = apiUrl.replace('/api/v1', '/ws').replace('http', 'ws');

    this.client = new Client({
      brokerURL: wsUrl,
      connectHeaders: this.getAuthHeaders(),
      debug: (str) => {
        if (!this.env.isProduction()) {
          console.debug('[WebSocket]', str);
        }
      },
      reconnectDelay: this.reconnectDelay,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        console.log('[WebSocket] Connected');
        this.connectionState.next(true);
        this.reconnectAttempts = 0;
        this.resubscribeAll();
      },
      onDisconnect: () => {
        console.log('[WebSocket] Disconnected');
        this.connectionState.next(false);
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame.headers['message']);
        this.connectionState.next(false);
      },
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        this.handleReconnect();
      }
    });

    this.client.activate();
  }

  disconnect(): void {
    if (this.client?.active) {
      // Unsubscribe from all topics
      this.subscriptions.forEach((sub, topic) => {
        try {
          sub.unsubscribe();
        } catch (e) {
          console.warn(`[WebSocket] Failed to unsubscribe from ${topic}:`, e);
        }
      });
      this.subscriptions.clear();

      // Close all message subjects
      this.messageSubjects.forEach((subject) => subject.complete());
      this.messageSubjects.clear();

      this.client.deactivate();
      this.connectionState.next(false);
    }
  }

  subscribeToRideTracking(rideId: number): Observable<RideTrackingUpdate> {
    const topic = `/topic/rides/${rideId}/tracking`;
    return this.subscribeTo<RideTrackingUpdate>(topic);
  }

  unsubscribeFromRideTracking(rideId: number): void {
    const topic = `/topic/rides/${rideId}/tracking`;
    this.unsubscribeFrom(topic);
  }

  private subscribeTo<T>(topic: string): Observable<T> {
    // If we already have a subject for this topic, return it
    if (this.messageSubjects.has(topic)) {
      return this.messageSubjects.get(topic)!.asObservable();
    }

    // Create a new subject for this topic
    const subject = new Subject<T>();
    this.messageSubjects.set(topic, subject);

    // If connected, subscribe immediately
    if (this.client?.active) {
      this.subscribeToTopic(topic);
    }
    // Otherwise, it will be subscribed when connection is established (resubscribeAll)

    return subject.asObservable();
  }

  private subscribeToTopic(topic: string): void {
    if (!this.client?.active || this.subscriptions.has(topic)) {
      return;
    }

    const subject = this.messageSubjects.get(topic);
    if (!subject) {
      return;
    }

    const subscription = this.client.subscribe(topic, (message: IMessage) => {
      try {
        const body = JSON.parse(message.body);
        subject.next(body);
      } catch (e) {
        console.error(`[WebSocket] Failed to parse message from ${topic}:`, e);
      }
    });

    this.subscriptions.set(topic, subscription);
    console.log(`[WebSocket] Subscribed to ${topic}`);
  }

  private unsubscribeFrom(topic: string): void {
    const subscription = this.subscriptions.get(topic);
    if (subscription) {
      try {
        subscription.unsubscribe();
      } catch (e) {
        console.warn(`[WebSocket] Failed to unsubscribe from ${topic}:`, e);
      }
      this.subscriptions.delete(topic);
    }

    const subject = this.messageSubjects.get(topic);
    if (subject) {
      subject.complete();
      this.messageSubjects.delete(topic);
    }

    console.log(`[WebSocket] Unsubscribed from ${topic}`);
  }

  private resubscribeAll(): void {
    // Resubscribe to all topics that have active subjects
    this.messageSubjects.forEach((_, topic) => {
      if (!this.subscriptions.has(topic)) {
        this.subscribeToTopic(topic);
      }
    });
  }

  private getAuthHeaders(): Record<string, string> {
    const token = localStorage.getItem('auth_token');
    if (token) {
      return { Authorization: `Bearer ${token}` };
    }
    return {};
  }

  private handleReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('[WebSocket] Max reconnection attempts reached');
      return;
    }

    this.reconnectAttempts++;
    const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
    console.log(`[WebSocket] Attempting reconnect in ${delay}ms (attempt ${this.reconnectAttempts})`);

    timer(delay)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => {
        if (!this.client?.active) {
          this.connect();
        }
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
    this.disconnect();
  }
}
