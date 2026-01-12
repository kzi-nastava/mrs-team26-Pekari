import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, OrderRideResponse, RideEstimateResponse } from '../../../core/services/ride-api.service';

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './passenger-home.component.html',
  styleUrl: './passenger-home.component.css'
})
export class PassengerHomeComponent implements OnInit, OnDestroy {
  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);

  estimate?: RideEstimateResponse;
  orderResult?: OrderRideResponse;
  error?: string;

  scheduledMin = '';
  scheduledMax = '';
  private scheduleBoundsTimer?: number;

  form = this.fb.group({
    pickup: this.fb.group({
      address: ['', Validators.required],
      latitude: [45.2671, Validators.required],
      longitude: [19.8335, Validators.required]
    }),
    dropoff: this.fb.group({
      address: ['', Validators.required],
      latitude: [45.255, Validators.required],
      longitude: [19.845, Validators.required]
    }),
    stops: this.fb.array([]),
    passengerEmails: [''],
    vehicleType: ['STANDARD', Validators.required],
    babyTransport: [false],
    petTransport: [false],
    scheduledAt: ['']
  });

  ngOnInit(): void {
    this.refreshScheduleBounds();
    this.scheduleBoundsTimer = window.setInterval(() => this.refreshScheduleBounds(), 60_000);

    // Convenience: show current time in the picker, but don't actually schedule unless user changes it.
    this.form.patchValue({ scheduledAt: this.scheduledMin }, { emitEvent: false });
  }

  ngOnDestroy(): void {
    if (this.scheduleBoundsTimer) {
      window.clearInterval(this.scheduleBoundsTimer);
    }
  }

  get stops(): FormArray {
    return this.form.get('stops') as FormArray;
  }

  addStop() {
    this.stops.push(
      this.fb.group({
        address: ['', Validators.required],
        latitude: [45.26, Validators.required],
        longitude: [19.84, Validators.required]
      })
    );
  }

  removeStop(index: number) {
    this.stops.removeAt(index);
  }

  setVehicleType(vehicleType: 'STANDARD' | 'VAN' | 'LUX') {
    this.form.patchValue({ vehicleType });
  }

  chooseFavoriteRoute() {
    this.error = 'Favorite routes are not implemented yet.';
  }

  private refreshScheduleBounds() {
    const now = new Date();
    const max = new Date(now.getTime() + 5 * 60 * 60 * 1000);
    this.scheduledMin = this.toDatetimeLocalValue(now);
    this.scheduledMax = this.toDatetimeLocalValue(max);

    // Keep the displayed default aligned with current time.
    // Scheduling is inferred from the selected time (must be at least 1 minute in the future).
    this.form.patchValue({ scheduledAt: this.scheduledMin }, { emitEvent: false });
  }

  private toDatetimeLocalValue(date: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    const yyyy = date.getFullYear();
    const mm = pad(date.getMonth() + 1);
    const dd = pad(date.getDate());
    const hh = pad(date.getHours());
    const min = pad(date.getMinutes());
    return `${yyyy}-${mm}-${dd}T${hh}:${min}`;
  }

  private normalizeScheduledAt(value: string): string {
    // datetime-local returns either 'YYYY-MM-DDTHH:mm' or 'YYYY-MM-DDTHH:mm:ss'
    // LocalDateTime on the backend parses reliably with seconds.
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(value)) {
      return `${value}:00`;
    }
    return value;
  }

  private parseDatetimeLocal(value: string): Date | undefined {
    // Accepts: YYYY-MM-DDTHH:mm or YYYY-MM-DDTHH:mm:ss
    const match = value.match(
      /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/
    );
    if (!match) return undefined;

    const year = Number(match[1]);
    const month = Number(match[2]);
    const day = Number(match[3]);
    const hour = Number(match[4]);
    const minute = Number(match[5]);
    const second = match[6] ? Number(match[6]) : 0;

    const date = new Date(year, month - 1, day, hour, minute, second, 0);
    if (Number.isNaN(date.getTime())) return undefined;
    return date;
  }

  private resolveScheduledAt(rawValue: string): { scheduledAt: string | null; error?: string } {
    const value = (rawValue || '').trim();
    if (!value) {
      return { scheduledAt: null };
    }

    const selected = this.parseDatetimeLocal(value);
    if (!selected) {
      return { scheduledAt: null, error: 'Invalid scheduled time.' };
    }

    const now = new Date();
    const max = new Date(now.getTime() + 5 * 60 * 60 * 1000);

    // Treat the prefilled current time as "Request Now".
    // Only schedule if the selected time is at least 1 minute in the future.
    const oneMinuteFromNow = now.getTime() + 60_000;
    if (selected.getTime() < oneMinuteFromNow) {
      return { scheduledAt: null };
    }

    if (selected.getTime() > max.getTime()) {
      return { scheduledAt: null, error: 'Scheduled time can be at most 5 hours in advance.' };
    }

    return { scheduledAt: this.normalizeScheduledAt(value) };
  }

  estimateRide() {
    this.error = undefined;
    this.orderResult = undefined;
    this.estimate = undefined;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fill all required fields.';
      return;
    }

    const value = this.form.getRawValue();

    this.rides
      .estimateRide({
        pickup: value.pickup as any,
        dropoff: value.dropoff as any,
        vehicleType: value.vehicleType as string,
        babyTransport: !!value.babyTransport,
        petTransport: !!value.petTransport
      })
      .subscribe({
        next: (resp) => {
          this.estimate = resp;
          this.cdr.detectChanges();
        },
        error: (err) => {
          const backendMsg = err?.error?.message;
          const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
          this.error = backendMsg || plainMsg || err?.message || 'Estimate failed';
          this.cdr.detectChanges();
        }
      });
  }

  orderRide() {
    this.error = undefined;
    this.orderResult = undefined;

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fill all required fields.';
      return;
    }

    const value = this.form.getRawValue();

    // Order endpoint is authenticated. Give an immediate, clear error if user isn't logged in.
    const token = localStorage.getItem('auth_token');
    if (!token) {
      this.error = 'Please log in to request a ride.';
      return;
    }

    const passengerEmails = (value.passengerEmails || '')
      .split(',')
      .map((e) => e.trim())
      .filter(Boolean);

    const scheduledResolved = this.resolveScheduledAt((value.scheduledAt || '').trim());
    if (scheduledResolved.error) {
      this.error = scheduledResolved.error;
      return;
    }

    this.rides
      .orderRide({
        pickup: value.pickup as any,
        stops: (value.stops as any[]) || [],
        dropoff: value.dropoff as any,
        passengerEmails,
        vehicleType: value.vehicleType as string,
        babyTransport: !!value.babyTransport,
        petTransport: !!value.petTransport,
        scheduledAt: scheduledResolved.scheduledAt
      })
      .subscribe({
        next: (resp) => {
          this.orderResult = resp;
          this.cdr.detectChanges();
        },
        error: (err) => {
          if (err?.status === 401 || err?.status === 403) {
            this.error = 'Please log in to request a ride.';
            this.cdr.detectChanges();
            return;
          }

          const backendMsg = err?.error?.message;
          const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
          this.error = backendMsg || plainMsg || err?.message || 'Order failed';
          this.cdr.detectChanges();
        }
      });
  }
}
