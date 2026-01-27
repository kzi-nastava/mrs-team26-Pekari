import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef, ViewChild, ViewChildren, QueryList, NgZone } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, OrderRideResponse, RideEstimateResponse, LocationPoint, FavoriteRoute, ActiveRideResponse } from '../../../core/services/ride-api.service';
import { EnvironmentService } from '../../../core/services/environment.service';
import { GeocodingService } from '../../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../../shared/components/address-autocomplete/address-autocomplete.component';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';
import { Client, IStompSocket } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { StompSubscription } from '@stomp/stompjs';

type FocusedInput = 'pickup' | 'dropoff' | { type: 'stop', index: number } | null;
interface RideTrackingMessage {
  rideId?: number;
  vehicleLatitude?: number | null;
  vehicleLongitude?: number | null;
  status?: string | null;
  updatedAt?: string | null;
}

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AddressAutocompleteComponent, RideMapComponent],
  templateUrl: './passenger-home.component.html',
  styleUrl: './passenger-home.component.css'
})
export class PassengerHomeComponent implements OnInit, OnDestroy {
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private env = inject(EnvironmentService);
  private geocoding = inject(GeocodingService);
  private cdr = inject(ChangeDetectorRef);
  private zone = inject(NgZone);

  private focusedInput: FocusedInput = null;
  private stompClient: Client | null = null;
  private trackingSubscription: StompSubscription | null = null;
  private trackingRideId: number | null = null;
  private activeRideRefreshTimer?: number;

  estimate?: RideEstimateResponse;
  orderResult?: OrderRideResponse;
  error?: string;
  isRideActive = false;
  favoriteRoutes: FavoriteRoute[] = [];
  showFavoriteRoutesModal = false;
  activeRide: ActiveRideResponse | null = null;
  trackingPosition: { latitude: number; longitude: number; updatedAt?: string | null } | null = null;
  trackingStatus?: string | null;
  trackingError?: string | null;

  scheduledMin = '';
  scheduledMax = '';
  private scheduleBoundsTimer?: number;

  // Map component inputs
  get mapPickup() {
    if (this.isRideActive && this.activeRide?.pickup) {
      return { latitude: this.activeRide.pickup.latitude, longitude: this.activeRide.pickup.longitude };
    }
    const pickup = this.form.get('pickup')?.value;
    return pickup?.latitude && pickup?.longitude ? { latitude: pickup.latitude, longitude: pickup.longitude } : null;
  }

  get mapDropoff() {
    if (this.isRideActive && this.activeRide?.dropoff) {
      return { latitude: this.activeRide.dropoff.latitude, longitude: this.activeRide.dropoff.longitude };
    }
    const dropoff = this.form.get('dropoff')?.value;
    return dropoff?.latitude && dropoff?.longitude ? { latitude: dropoff.latitude, longitude: dropoff.longitude } : null;
  }

  get mapStops() {
    if (this.isRideActive && this.activeRide?.stops) {
      return this.activeRide.stops.map((stop, index) => ({
        latitude: stop.latitude,
        longitude: stop.longitude,
        label: `Stop ${index + 1}`
      }));
    }
    const stops = this.form.get('stops')?.value as any[] || [];
    return stops
      .filter(stop => stop?.latitude && stop?.longitude)
      .map((stop, index) => ({
        latitude: stop.latitude,
        longitude: stop.longitude,
        label: `Stop ${index + 1}`
      }));
  }

  get mapRoutePoints() {
    if (this.isRideActive && this.activeRide) {
      const points: Array<{ latitude: number; longitude: number }> = [];
      if (this.activeRide.pickup) {
        points.push({
          latitude: this.activeRide.pickup.latitude,
          longitude: this.activeRide.pickup.longitude
        });
      }
      if (this.activeRide.stops) {
        this.activeRide.stops.forEach((stop) => {
          points.push({ latitude: stop.latitude, longitude: stop.longitude });
        });
      }
      if (this.activeRide.dropoff) {
        points.push({
          latitude: this.activeRide.dropoff.latitude,
          longitude: this.activeRide.dropoff.longitude
        });
      }
      return points;
    }
    const points: Array<{ latitude: number; longitude: number }> = [];
    const value = this.form.getRawValue();

    if (value.pickup?.latitude && value.pickup?.longitude) {
      points.push({ latitude: value.pickup.latitude, longitude: value.pickup.longitude });
    }

    const stops = value.stops as any[] || [];
    stops.forEach(stop => {
      if (stop?.latitude && stop?.longitude) {
        points.push({ latitude: stop.latitude, longitude: stop.longitude });
      }
    });

    if (value.dropoff?.latitude && value.dropoff?.longitude) {
      points.push({ latitude: value.dropoff.latitude, longitude: value.dropoff.longitude });
    }

    return points;
  }

  form = this.fb.group({
    pickup: this.fb.group({
      address: ['', Validators.required],
      latitude: [null as number | null, Validators.required],
      longitude: [null as number | null, Validators.required]
    }),
    dropoff: this.fb.group({
      address: ['', Validators.required],
      latitude: [null as number | null, Validators.required],
      longitude: [null as number | null, Validators.required]
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
    this.activeRideRefreshTimer = window.setInterval(() => this.refreshActiveRide(), 10_000);

    // Convenience: show current time in the picker, but don't actually schedule unless user changes it.
    this.form.patchValue({ scheduledAt: this.scheduledMin }, { emitEvent: false });

    // Subscribe to form changes
    this.form.valueChanges.subscribe(() => {
      this.estimate = undefined;
      this.error = undefined;
    });

    // Load favorite routes
    this.loadFavoriteRoutes();
    this.refreshActiveRide(true);
  }

  ngOnDestroy(): void {
    if (this.scheduleBoundsTimer) {
      window.clearInterval(this.scheduleBoundsTimer);
    }
    if (this.activeRideRefreshTimer) {
      window.clearInterval(this.activeRideRefreshTimer);
    }
    this.stopTracking();
  }

  get stops(): FormArray {
    return this.form.get('stops') as FormArray;
  }

  addStop() {
    const newIndex = this.stops.length;
    this.stops.push(
      this.fb.group({
        address: ['', Validators.required],
        latitude: [null as number | null, Validators.required],
        longitude: [null as number | null, Validators.required]
      })
    );
    // Automatically focus the newly added stop
    this.focusedInput = { type: 'stop', index: newIndex };
  }

  removeStop(index: number) {
    this.stops.removeAt(index);
    // Map markers will be updated automatically via form valueChanges
  }

  clearPickup() {
    this.form.patchValue({
      pickup: { address: '', latitude: null, longitude: null }
    });
    if (this.pickupAutocomplete) {
      this.pickupAutocomplete.setAddress('');
    }
  }

  clearDropoff() {
    this.form.patchValue({
      dropoff: { address: '', latitude: null, longitude: null }
    });
    if (this.dropoffAutocomplete) {
      this.dropoffAutocomplete.setAddress('');
    }
  }

  setVehicleType(vehicleType: 'STANDARD' | 'VAN' | 'LUX') {
    this.form.patchValue({ vehicleType });
  }

  onPickupFocus(): void {
    this.focusedInput = 'pickup';
  }

  onDropoffFocus(): void {
    this.focusedInput = 'dropoff';
  }

  onStopFocus(index: number): void {
    this.focusedInput = { type: 'stop', index };
  }

  loadFavoriteRoutes() {
    const token = localStorage.getItem('auth_token');
    if (!token) {
      return;
    }

    this.rides.getFavoriteRoutes().subscribe({
      next: (routes:any) => {
        this.favoriteRoutes = routes;
        this.cdr.detectChanges();
      },
      error: (err:any) => {
        // Silently fail - user might not have any favorite routes yet
        console.debug('Failed to load favorite routes:', err);
      }
    });
  }

  chooseFavoriteRoute() {
    if (this.favoriteRoutes.length === 0) {
      this.error = 'You don\'t have any favorite routes yet. Complete a ride and add it to favorites from your ride history.';
      return;
    }
    this.showFavoriteRoutesModal = true;
    this.cdr.detectChanges();
  }

  selectFavoriteRoute(route: FavoriteRoute) {
    this.showFavoriteRoutesModal = false;
    this.error = undefined;

    // Clear existing stops
    while (this.stops.length > 0) {
      this.stops.removeAt(0);
    }

    // Set pickup
    this.form.patchValue({
      pickup: {
        address: route.pickup.address,
        latitude: route.pickup.latitude,
        longitude: route.pickup.longitude
      }
    });

    // Set stops
    if (route.stops && route.stops.length > 0) {
      route.stops.forEach(stop => {
        this.stops.push(
          this.fb.group({
            address: [stop.address, Validators.required],
            latitude: [stop.latitude, Validators.required],
            longitude: [stop.longitude, Validators.required]
          })
        );
      });
    }

    // Set dropoff
    this.form.patchValue({
      dropoff: {
        address: route.dropoff.address,
        latitude: route.dropoff.latitude,
        longitude: route.dropoff.longitude
      }
    });

    // Set vehicle type and options
    this.form.patchValue({
      vehicleType: route.vehicleType || 'STANDARD',
      babyTransport: route.babyTransport || false,
      petTransport: route.petTransport || false
    });

    // Update autocomplete components
    if (this.pickupAutocomplete) {
      this.pickupAutocomplete.setAddress(route.pickup.address);
    }
    if (this.dropoffAutocomplete) {
      this.dropoffAutocomplete.setAddress(route.dropoff.address);
    }

    // Update stop autocompletes
    setTimeout(() => {
      if (this.stopAutocompletes && route.stops) {
        route.stops.forEach((stop, index) => {
          const autocomplete = this.stopAutocompletes?.toArray()[index];
          if (autocomplete) {
            autocomplete.setAddress(stop.address);
          }
        });
      }
    }, 0);

    this.cdr.detectChanges();
  }

  closeFavoriteRoutesModal() {
    this.showFavoriteRoutesModal = false;
    this.cdr.detectChanges();
  }

  onMapClick(event: { latitude: number; longitude: number }): void {
    if (this.isRideActive) {
      return;
    }

    // Determine which field to update
    let targetField: 'pickup' | 'dropoff' | { type: 'stop', index: number };

    if (this.focusedInput === 'pickup') {
      targetField = 'pickup';
    } else if (this.focusedInput === 'dropoff') {
      targetField = 'dropoff';
    } else if (this.focusedInput && typeof this.focusedInput === 'object' && this.focusedInput.type === 'stop') {
      targetField = this.focusedInput;
    } else {
      const pickupAddress = this.form.get('pickup.address')?.value;
      const dropoffAddress = this.form.get('dropoff.address')?.value;

      if (!pickupAddress || pickupAddress.trim() === '') {
        targetField = 'pickup';
      } else if (!dropoffAddress || dropoffAddress.trim() === '') {
        targetField = 'dropoff';
      } else {
        targetField = 'dropoff';
      }
    }

    // Update coordinates immediately with placeholder address
    const placeholder = 'Loading address...';
    if (targetField === 'pickup') {
      this.updatePickupLocation(placeholder, event.latitude, event.longitude);
    } else if (targetField === 'dropoff') {
      this.updateDropoffLocation(placeholder, event.latitude, event.longitude);
    } else {
      this.updateStopLocation(targetField.index, placeholder, event.latitude, event.longitude);
    }

    // Fetch address in background and update when ready
    this.geocoding.reverseGeocode(event.latitude, event.longitude).subscribe({
      next: (result:any) => {
        if (!result) return;

        const address = result.displayName;
        setTimeout(() => {
          if (targetField === 'pickup') {
            this.updatePickupLocation(address, event.latitude, event.longitude);
          } else if (targetField === 'dropoff') {
            this.updateDropoffLocation(address, event.latitude, event.longitude);
          } else {
            this.updateStopLocation(targetField.index, address, event.latitude, event.longitude);
          }
        }, 0);
      },
      error: () => {
        // On error, use coordinates as address
        const fallbackAddress = `${event.latitude.toFixed(6)}, ${event.longitude.toFixed(6)}`;
        setTimeout(() => {
          if (targetField === 'pickup') {
            this.updatePickupLocation(fallbackAddress, event.latitude, event.longitude);
          } else if (targetField === 'dropoff') {
            this.updateDropoffLocation(fallbackAddress, event.latitude, event.longitude);
          } else {
            this.updateStopLocation(targetField.index, fallbackAddress, event.latitude, event.longitude);
          }
        }, 0);
      }
    });
  }

  onPickupSelected(selection: AddressSelection): void {
    this.updatePickupLocation(selection.address, selection.latitude, selection.longitude);
  }

  onDropoffSelected(selection: AddressSelection): void {
    this.updateDropoffLocation(selection.address, selection.latitude, selection.longitude);
  }

  onStopSelected(index: number, selection: AddressSelection): void {
    this.updateStopLocation(index, selection.address, selection.latitude, selection.longitude);
  }

  private updatePickupLocation(address: string, lat: number, lng: number): void {
    this.form.patchValue({
      pickup: { address, latitude: lat, longitude: lng }
    });

    if (this.pickupAutocomplete) {
      this.pickupAutocomplete.setAddress(address);
    }
  }

  private updateDropoffLocation(address: string, lat: number, lng: number): void {
    this.form.patchValue({
      dropoff: { address, latitude: lat, longitude: lng }
    });

    if (this.dropoffAutocomplete) {
      this.dropoffAutocomplete.setAddress(address);
    }
  }

  private updateStopLocation(index: number, address: string, lat: number, lng: number): void {
    const stopsArray = this.stops;
    if (index >= 0 && index < stopsArray.length) {
      const stopGroup = stopsArray.at(index);
      stopGroup?.patchValue({ address, latitude: lat, longitude: lng });

      const stopAutocomplete = this.stopAutocompletes?.toArray()[index];
      if (stopAutocomplete) {
        stopAutocomplete.setAddress(address);
      }
    }
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
    this.clearMessages();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fill all required fields.';
      return;
    }

    const value = this.form.getRawValue();

    const pickup = value.pickup;
    const dropoff = value.dropoff;

    if (!pickup?.latitude || !pickup?.longitude || !pickup?.address ||
        !dropoff?.latitude || !dropoff?.longitude || !dropoff?.address) {
      this.error = 'Please fill all required location fields.';
      return;
    }

    const stops = (value.stops || [])
      .filter((s: any): s is LocationPoint =>
        !!s && !!s.latitude && !!s.longitude && !!s.address
      );

    this.rides
      .estimateRide({
        pickup: pickup as LocationPoint,
        stops,
        dropoff: dropoff as LocationPoint,
        vehicleType: value.vehicleType || 'STANDARD'
      })
      .subscribe({
        next: (resp:any) => {
          this.estimate = resp;
          this.cdr.detectChanges();
        },
        error: (err:any) => {
          const backendMsg = err?.error?.message;
          const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
          this.error = backendMsg || plainMsg || err?.message || 'Estimate failed';
          this.cdr.detectChanges();
        }
      });
  }

  orderRide() {
    this.clearMessages();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fill all required fields.';
      return;
    }

    const value = this.form.getRawValue();

    const passengerEmails = (value.passengerEmails || '')
      .split(',')
      .map((e:any) => e.trim())
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
        next: (resp:any) => {
          this.orderResult = resp;
          // Mark ride as active if successfully ordered
          if (resp.status === 'ACCEPTED' || resp.status === 'PENDING') {
            this.isRideActive = true;
            this.form.disable(); // Disable form when ride is active
          }
          this.refreshActiveRide(true);
          this.cdr.detectChanges();
        },
        error: (err:any) => {
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

  private clearMessages(): void {
    this.error = undefined;
    this.orderResult = undefined;
    this.estimate = undefined;
  }

  cancelRide(): void {
    if (!this.orderResult?.rideId) {
      this.error = 'No active ride to cancel.';
      return;
    }

    const reason = 'Cancelled by passenger'; // Default reason

    this.rides.cancelRide(this.orderResult.rideId, reason).subscribe({
      next: (response:any) => {
        // Mark ride as no longer active, but keep the modal visible
        this.isRideActive = false;
        this.form.enable();
        this.activeRide = null;
        this.trackingPosition = null;
        this.trackingStatus = null;
        this.trackingError = null;
        this.stopTracking();

        // Update the order result to show cancelled status
        this.orderResult = {
          ...this.orderResult!,
          status: 'CANCELLED',
          message: response.message || 'Ride cancelled successfully'
        };

        this.cdr.detectChanges();
      },
      error: (err:any) => {
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error = backendMsg || plainMsg || err?.message || 'Failed to cancel ride';
        this.cdr.detectChanges();
      }
    });
  }

  resetForm(): void {
    this.isRideActive = false;
    this.form.enable();
    this.clearMessages();
    this.cdr.detectChanges();
  }

  get trackingDrivers() {
    if (!this.trackingPosition) {
      return [];
    }
    return [
      {
        latitude: this.trackingPosition.latitude,
        longitude: this.trackingPosition.longitude,
        busy: true,
        popupContent: 'Driver'
      }
    ];
  }

  get trackingCenter(): [number, number] {
    if (this.trackingPosition) {
      return [this.trackingPosition.latitude, this.trackingPosition.longitude];
    }
    if (this.activeRide?.pickup) {
      return [this.activeRide.pickup.latitude, this.activeRide.pickup.longitude];
    }
    return [45.2671, 19.8335];
  }

  private refreshActiveRide(force = false) {
    if (!force && !this.isRideActive && !this.orderResult?.rideId) {
      return;
    }

    this.rides.getActiveRideForPassenger().subscribe({
      next: (ride) => {
        this.activeRide = ride;
        this.trackingError = null;
        this.isRideActive = true;
        this.form.disable();
        this.handleTrackingState();
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (err.status === 204) {
          this.activeRide = null;
          this.trackingPosition = null;
          this.trackingStatus = null;
          this.trackingError = null;
          this.isRideActive = false;
          this.form.enable();
          this.stopTracking();
        } else {
          this.trackingError = 'Failed to load active ride tracking.';
          console.error('Error loading active ride:', err);
        }
        this.cdr.detectChanges();
      }
    });
  }

  private handleTrackingState() {
    if (!this.activeRide || this.activeRide.status !== 'IN_PROGRESS') {
      this.trackingPosition = null;
      this.trackingStatus = null;
      this.stopTracking();
      return;
    }

    this.startTracking(this.activeRide.rideId);
  }

  private startTracking(rideId: number) {
    if (this.trackingRideId === rideId && this.stompClient?.connected) {
      return;
    }

    this.stopTracking();
    this.trackingRideId = rideId;

    const wsUrl = this.getWebSocketUrl();
    const token = localStorage.getItem('auth_token');

    this.stompClient = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as IStompSocket,
      reconnectDelay: 5000,
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      onConnect: () => {
        this.trackingSubscription = this.stompClient?.subscribe(
          `/topic/rides/${rideId}/tracking`,
          (message) => this.handleTrackingMessage(message.body)
        ) || null;
      },
      onStompError: (frame) => {
        this.zone.run(() => {
          this.trackingError = frame.headers['message'] || 'Tracking connection error.';
          this.cdr.detectChanges();
        });
      }
    });

    this.stompClient.activate();
  }

  private handleTrackingMessage(raw: string) {
    let payload: RideTrackingMessage | null = null;
    try {
      payload = JSON.parse(raw);
    } catch (error) {
      console.warn('Failed to parse tracking message:', error);
      return;
    }

    if (!payload || payload.vehicleLatitude == null || payload.vehicleLongitude == null) {
      return;
    }

    this.zone.run(() => {
      this.trackingPosition = {
        latitude: payload.vehicleLatitude ?? 0,
        longitude: payload.vehicleLongitude ?? 0,
        updatedAt: payload.updatedAt ?? null
      };
      this.trackingStatus = payload.status ?? null;
      this.trackingError = null;
      this.cdr.detectChanges();
    });
  }

  private stopTracking() {
    if (this.trackingSubscription) {
      this.trackingSubscription.unsubscribe();
      this.trackingSubscription = null;
    }
    if (this.stompClient) {
      this.stompClient.deactivate();
      this.stompClient = null;
    }
    this.trackingRideId = null;
  }

  private getWebSocketUrl(): string {
    const apiUrl = this.env.getApiUrl();
    const baseUrl = apiUrl.replace(/\/api\/v1\/?$/, '');
    const wsBase = baseUrl.replace(/^http/, 'ws');
    return `${wsBase}/ws`;
  }
}
