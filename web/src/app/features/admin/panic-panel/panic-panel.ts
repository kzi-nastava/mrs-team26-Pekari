import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RideApiService, DriverRideHistoryResponse } from '../../../core/services/ride-api.service';

@Component({
  selector: 'app-panic-panel',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './panic-panel.html',
  styleUrl: './panic-panel.css',
})
export class PanicPanel implements OnInit, OnDestroy {
  private rideService = inject(RideApiService);
  private pollTimer: ReturnType<typeof setInterval> | null = null;
  private audioContext: AudioContext | null = null;
  private audio: HTMLAudioElement | null = null;

  panicRides: DriverRideHistoryResponse[] = [];
  loading = false;
  error: string | null = null;

  ngOnInit() {
    this.loadPanicRides();
    this.startPolling();
    this.setupAudio();
  }

  ngOnDestroy() {
    this.stopPolling();
    if (this.audio) {
      this.audio.pause();
      this.audio = null;
    }
  }

  private setupAudio() {
    // Create a simple beep sound using Web Audio API for notifications
    this.audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
  }

  private playNotificationSound() {
    if (!this.audioContext) return;

    try {
      const oscillator = this.audioContext.createOscillator();
      const gainNode = this.audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(this.audioContext.destination);

      oscillator.frequency.value = 800;
      oscillator.type = 'sine';

      gainNode.gain.setValueAtTime(0.3, this.audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, this.audioContext.currentTime + 0.5);

      oscillator.start(this.audioContext.currentTime);
      oscillator.stop(this.audioContext.currentTime + 0.5);

      // Play twice for emphasis
      setTimeout(() => {
        const oscillator2 = this.audioContext!.createOscillator();
        const gainNode2 = this.audioContext!.createGain();

        oscillator2.connect(gainNode2);
        gainNode2.connect(this.audioContext!.destination);

        oscillator2.frequency.value = 1000;
        oscillator2.type = 'sine';

        gainNode2.gain.setValueAtTime(0.3, this.audioContext!.currentTime);
        gainNode2.gain.exponentialRampToValueAtTime(0.01, this.audioContext!.currentTime + 0.5);

        oscillator2.start(this.audioContext!.currentTime);
        oscillator2.stop(this.audioContext!.currentTime + 0.5);
      }, 200);
    } catch (e) {
      console.error('Failed to play notification sound:', e);
    }
  }

  loadPanicRides() {
    this.loading = true;
    this.error = null;

    this.rideService.getActivePanicRides().subscribe({
      next: (rides) => {
        const previousCount = this.panicRides.length;
        this.panicRides = rides;
        this.loading = false;

        // If new panic rides detected, play sound and show notification
        if (rides.length > previousCount && previousCount >= 0) {
          this.playNotificationSound();
          this.showBrowserNotification(rides.length - previousCount);
        }
      },
      error: (err) => {
        this.error = 'Failed to load panic rides. Please try again.';
        console.error('Error loading panic rides:', err);
        this.loading = false;
      }
    });
  }

  private showBrowserNotification(newCount: number) {
    if ('Notification' in window && Notification.permission === 'granted') {
      new Notification('🚨 PANIC Alert!', {
        body: `${newCount} new panic ${newCount === 1 ? 'alert' : 'alerts'} detected!`,
        icon: '/favicon.ico',
        requireInteraction: true
      });
    } else if ('Notification' in window && Notification.permission !== 'denied') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          new Notification('🚨 PANIC Alert!', {
            body: `${newCount} new panic ${newCount === 1 ? 'alert' : 'alerts'} detected!`,
            icon: '/favicon.ico',
            requireInteraction: true
          });
        }
      });
    }
  }

  private startPolling() {
    // Poll every 5 seconds for new panic activations
    this.pollTimer = setInterval(() => {
      this.rideService.getActivePanicRides().subscribe({
        next: (rides) => {
          const previousCount = this.panicRides.length;
          this.panicRides = rides;

          // If new panic rides detected, play sound and show notification
          if (rides.length > previousCount) {
            this.playNotificationSound();
            this.showBrowserNotification(rides.length - previousCount);
          }
        },
        error: (err) => {
          console.error('Error polling for panic rides:', err);
        }
      });
    }, 5000);
  }

  private stopPolling() {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = null;
    }
  }

  getStatusLabel(status: string): string {
    switch (status) {
      case 'ACCEPTED': return 'Assigned';
      case 'SCHEDULED': return 'Scheduled';
      case 'IN_PROGRESS': return 'In Progress';
      case 'STOP_REQUESTED': return 'Stop Requested';
      case 'COMPLETED': return 'Completed';
      case 'CANCELLED': return 'Cancelled';
      default: return status;
    }
  }

  getStatusClass(status: string): string {
    switch (status) {
      case 'ACCEPTED': return 'status-assigned';
      case 'SCHEDULED': return 'status-scheduled';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'STOP_REQUESTED': return 'status-stop-requested';
      case 'COMPLETED': return 'status-completed';
      case 'CANCELLED': return 'status-cancelled';
      default: return '';
    }
  }

  formatDateTime(dateTime: string | null): string {
    if (!dateTime) return 'N/A';
    return new Date(dateTime).toLocaleString();
  }
}
