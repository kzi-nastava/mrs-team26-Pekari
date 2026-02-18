import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef, ViewChild, ViewChildren, QueryList, ElementRef } from '@angular/core';
import { FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { filter, take } from 'rxjs/operators';
import { RideApiService, OrderRideResponse, RideEstimateResponse, LocationPoint, FavoriteRoute } from '../../../core/services/ride-api.service';
import { GeocodingService } from '../../../core/services/geocoding.service';
import { WebSocketService, RideTrackingUpdate } from '../../../core/services/websocket.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../../shared/components/address-autocomplete/address-autocomplete.component';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';

type FocusedInput = 'pickup' | 'dropoff' | { type: 'stop', index: number } | null;

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, AddressAutocompleteComponent, RideMapComponent],
  templateUrl: './passenger-home.component.html',
  styleUrl: './passenger-home.component.css'
})
export class PassengerHomeComponent implements OnInit, OnDestroy {
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('errorMessage', { read: ElementRef }) errorMessageEl?: ElementRef<HTMLElement>;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private geocoding = inject(GeocodingService);
  private wsService = inject(WebSocketService);
  private cdr = inject(ChangeDetectorRef);

  private focusedInput: FocusedInput = null;
  private trackingSubscription?: Subscription;
  private pollTimer?: ReturnType<typeof setInterval>;

  estimate?: RideEstimateResponse;
  orderResult?: OrderRideResponse;
  error?: string;
  isRideActive = false;
  rideStatus?: string;
  stopRequested = false;
  panicActivated = false;
  favoriteRoutes: FavoriteRoute[] = [];
  showFavoriteRoutesModal = false;

  // Tracking state
  driverLocation?: { latitude: number; longitude: number } | null = null;
  estimatedTimeMinutes?: number;
  showReportForm = false;
  reportText = '';
  reportSubmitting = false;
  reportSuccess?: string;

  // Rating state
  showRatingModal = false;
  vehicleRating = 0;
  driverRating = 0;
  ratingComment = '';
  ratingSubmitting = false;
  ratingSuccess?: string;
  completedRideId?: number;

  scheduledMin = '';
  scheduledMax = '';
  private scheduleBoundsTimer?: number;

  // Map component inputs
  get mapPickup() {
    const pickup = this.form.get('pickup')?.value;
    return pickup?.latitude && pickup?.longitude ? { latitude: pickup.latitude, longitude: pickup.longitude } : null;
  }

  get mapDropoff() {
    const dropoff = this.form.get('dropoff')?.value;
    return dropoff?.latitude && dropoff?.longitude ? { latitude: dropoff.latitude, longitude: dropoff.longitude } : null;
  }

  get mapStops() {
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

    // Subscribe to form changes
    this.form.valueChanges.subscribe(() => {
      this.estimate = undefined;
      this.error = undefined;
    });

    // Check for active ride on init
    this.checkForActiveRide();

    // Load favorite routes
    this.loadFavoriteRoutes();
  }

  private checkForActiveRide(): void {
    this.rides.getActiveRideForPassenger().subscribe({
      next: (activeRide) => {
        if (activeRide) {
          // Convert active ride to order result format to show the modal
          this.orderResult = {
            rideId: activeRide.rideId,
            status: activeRide.status,
            message: 'Active ride in progress',
            estimatedPrice: activeRide.estimatedPrice,
            scheduledAt: activeRide.scheduledAt,
            assignedDriverEmail: activeRide.driver?.email
          };
          this.isRideActive = true;
          this.rideStatus = activeRide.status;
          this.form.disable();

          // Start tracking the active ride
          this.startTracking(activeRide.rideId);
          this.startPolling();

          this.cdr.detectChanges();
        }
      },
      error: (err) => {
        if (err?.status !== 404) {
          console.error('Error checking for active ride:', err);
        }
      }
    });
  }

  private startPolling(): void {
    this.stopPolling();
    // Poll every 5 seconds to check for ride status updates
    this.pollTimer = setInterval(() => {
      if (this.isRideActive && this.orderResult?.rideId) {
        this.rides.getActiveRideForPassenger().subscribe({
          next: (updatedRide) => {
            if (updatedRide) {
              // Update status
              this.rideStatus = updatedRide.status;
              this.stopRequested = updatedRide.status === 'STOP_REQUESTED';

              // Update order result
              this.orderResult = {
                ...this.orderResult!,
                status: updatedRide.status,
                assignedDriverEmail: updatedRide.driver?.email
              };

              // Check for completion
              if (updatedRide.status === 'COMPLETED') {
                this.handleRideCompletion(updatedRide.rideId);
                this.stopPolling();
              }

              this.cdr.detectChanges();
            } else {
              // No active ride returned - check if ride was completed
              // The backend doesn't return COMPLETED rides, so if we had an active ride
              // and now there's nothing, it was either completed or cancelled
              if (this.rideStatus === 'IN_PROGRESS' || this.rideStatus === 'STOP_REQUESTED') {
                // Likely completed, show rating modal
                this.handleRideCompletion(this.orderResult!.rideId);
              }
              this.stopPolling();
            }
          },
          error: (err) => {
            if (err?.status === 404 || err?.status === 204) {
              // No active ride - check if it was completed
              if (this.rideStatus === 'IN_PROGRESS' || this.rideStatus === 'STOP_REQUESTED') {
                this.handleRideCompletion(this.orderResult!.rideId);
              }
              this.stopPolling();
            }
            console.error('Error polling for ride updates:', err);
          }
        });
      }
    }, 5000);
  }

  private stopPolling(): void {
    if (this.pollTimer) {
      clearInterval(this.pollTimer);
      this.pollTimer = undefined;
    }
  }

  ngOnDestroy(): void {
    if (this.scheduleBoundsTimer) {
      window.clearInterval(this.scheduleBoundsTimer);
    }
    this.stopTracking();
    this.stopPolling();
  }

  private startTracking(rideId: number): void {
    this.stopTracking();
    console.log('[PassengerHome] Starting tracking for ride:', rideId);
    this.wsService.connect();

    // Wait for connection before subscribing
    this.wsService.isConnected$.pipe(
      filter(connected => connected),
      take(1)
    ).subscribe(() => {
      if (!this.trackingSubscription) {
        console.log('[PassengerHome] WebSocket connected, subscribing to tracking');
        this.trackingSubscription = this.wsService.subscribeToRideTracking(rideId).subscribe({
          next: (update: RideTrackingUpdate) => {
            console.log('[PassengerHome] Received tracking update:', update);

            // Check if ride is completed (check both rideStatus and status fields)
            if (update.rideStatus === 'COMPLETED' || update.status === 'COMPLETED') {
              console.log('[PassengerHome] Ride completed, showing rating modal');
              this.handleRideCompletion(rideId);
              return;
            }

            // Only update location if we have valid coordinates
            if (update.vehicleLatitude && update.vehicleLongitude) {
              this.driverLocation = {
                latitude: update.vehicleLatitude,
                longitude: update.vehicleLongitude
              };
              this.estimatedTimeMinutes = update.estimatedTimeToDestinationMinutes;
              console.log('[PassengerHome] Driver location set to:', this.driverLocation);
            }
            this.cdr.detectChanges();
          },
          error: (err) => {
            console.error('[PassengerHome] Tracking subscription error:', err);
          }
        });
      }
    });
  }

  private stopTracking(): void {
    if (this.trackingSubscription) {
      this.trackingSubscription.unsubscribe();
      this.trackingSubscription = undefined;
    }
    if (this.orderResult?.rideId) {
      this.wsService.unsubscribeFromRideTracking(this.orderResult.rideId);
    }
    this.driverLocation = null;
    this.estimatedTimeMinutes = undefined;
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
    this.rides.getFavoriteRoutes().subscribe({
      next: (routes) => {
        this.favoriteRoutes = routes;
        this.cdr.detectChanges();
      },
      error: (err) => {
        // Silently fail - user might not have any favorite routes yet
        console.debug('Failed to load favorite routes:', err);
      }
    });
  }

  chooseFavoriteRoute() {
    if (this.favoriteRoutes.length === 0) {
      this.error = 'You don\'t have any favorite routes yet. Complete a ride and add it to favorites from your ride history.';
      this.cdr.detectChanges();
      setTimeout(() => {
        this.errorMessageEl?.nativeElement?.scrollIntoView({ behavior: 'smooth', block: 'center' });
      }, 0);
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
        address: route.pickup?.address || '',
        latitude: route.pickup?.latitude || 0,
        longitude: route.pickup?.longitude || 0
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
        address: route.dropoff?.address || '',
        latitude: route.dropoff?.latitude || 0,
        longitude: route.dropoff?.longitude || 0
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
      this.pickupAutocomplete.setAddress(route.pickup?.address || '');
    }
    if (this.dropoffAutocomplete) {
      this.dropoffAutocomplete.setAddress(route.dropoff?.address || '');
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
      next: (result) => {
        if (!result) return;

        const address = result.displayName;
        if (targetField === 'pickup') {
          this.updatePickupLocation(address, event.latitude, event.longitude);
        } else if (targetField === 'dropoff') {
          this.updateDropoffLocation(address, event.latitude, event.longitude);
        } else {
          this.updateStopLocation(targetField.index, address, event.latitude, event.longitude);
        }
      },
      error: () => {
        // On error, use coordinates as address
        const fallbackAddress = `${event.latitude.toFixed(6)}, ${event.longitude.toFixed(6)}`;
        if (targetField === 'pickup') {
          this.updatePickupLocation(fallbackAddress, event.latitude, event.longitude);
        } else if (targetField === 'dropoff') {
          this.updateDropoffLocation(fallbackAddress, event.latitude, event.longitude);
        } else {
          this.updateStopLocation(targetField.index, fallbackAddress, event.latitude, event.longitude);
        }
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
    const newMin = this.toDatetimeLocalValue(now);
    const newMax = this.toDatetimeLocalValue(max);

    if (this.scheduledMin !== newMin) {
      this.scheduledMin = newMin;
    }
    if (this.scheduledMax !== newMax) {
      this.scheduledMax = newMax;
    }

    // Keep the displayed default aligned with current time if no ride is active.
    if (!this.isRideActive) {
      this.form.patchValue({ scheduledAt: this.scheduledMin }, { emitEvent: false });
    }
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
    this.clearMessages();

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error = 'Please fill all required fields.';
      return;
    }

    const value = this.form.getRawValue();

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
          // Mark ride as active if successfully ordered
          if (resp.status === 'ACCEPTED' || resp.status === 'PENDING') {
            this.isRideActive = true;
            this.rideStatus = resp.status;
            this.form.disable(); // Disable form when ride is active

            // Start tracking the ride
            this.startTracking(resp.rideId);
            this.startPolling();
          }
          this.cdr.detectChanges();
        },
        error: (err) => {
          if (err?.status === 401) {
            this.error = 'Please log in to request a ride.';
            this.cdr.detectChanges();
            return;
          }
          if (err?.status === 403 && err?.error?.code === 'USER_BLOCKED') {
            this.error = err?.error?.message || 'You have been blocked and cannot order rides. Contact support.';
            this.cdr.detectChanges();
            return;
          }
          if (err?.status === 403) {
            this.error = 'You do not have permission to request a ride.';
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
      next: (response) => {
        // Stop tracking
        this.resetForm();
        this.stopTracking();

        // Mark ride as no longer active, but keep the modal visible
        this.isRideActive = false;
        this.form.enable();

        // Update the order result to show cancelled status
        this.orderResult = {
          ...this.orderResult!,
          status: 'CANCELLED',
          message: response.message || 'Ride cancelled successfully'
        };

        this.cdr.detectChanges();
      },
      error: (err) => {
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error = backendMsg || plainMsg || err?.message || 'Failed to cancel ride';
        this.cdr.detectChanges();
      }
    });
  }

  requestStopRide(): void {
    if (!this.orderResult?.rideId) {
      this.error = 'No active ride to stop.';
      return;
    }

    this.rides.requestStopRide(this.orderResult.rideId).subscribe({
      next: (response) => {
        this.error = undefined;
        this.stopRequested = true;
        this.rideStatus = 'STOP_REQUESTED';
        // Show message to user that stop was requested
        if (this.orderResult) {
          this.orderResult.status = 'STOP_REQUESTED';
          this.orderResult.message = 'Stop requested. Driver will stop at the nearest safe location.';
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error = backendMsg || plainMsg || err?.message || 'Failed to request stop';
        this.cdr.detectChanges();
      }
    });
  }

  resetForm(): void {
    this.stopTracking();
    this.stopPolling();
    this.isRideActive = false;
    this.rideStatus = undefined;
    this.orderResult = undefined;
    this.stopRequested = false;
    this.form.enable();
    this.clearMessages();
    this.showReportForm = false;
    this.reportText = '';
    this.reportSuccess = undefined;
    this.cdr.detectChanges();
  }

  toggleReportForm(): void {
    this.showReportForm = !this.showReportForm;
    this.reportSuccess = undefined;
  }

  submitReport(): void {
    if (!this.orderResult?.rideId || !this.reportText.trim()) {
      return;
    }

    this.reportSubmitting = true;
    this.reportSuccess = undefined;
    this.error = undefined;

    this.rides.reportInconsistency(this.orderResult.rideId, this.reportText.trim()).subscribe({
      next: (response) => {
        this.reportSubmitting = false;
        this.reportSuccess = response.message || 'Report submitted successfully';
        this.reportText = '';
        this.showReportForm = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.reportSubmitting = false;
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error = backendMsg || plainMsg || err?.message || 'Failed to submit report';
        this.cdr.detectChanges();
      }
    });
  }

  activatePanic(): void {
    if (!this.orderResult?.rideId) {
      this.error = 'No active ride to report panic.';
      return;
    }

    this.rides.activatePanic(this.orderResult.rideId).subscribe({
      next: (response) => {
        this.panicActivated = true;
        this.cdr.detectChanges();
      },
      error: (err) => {
        const backendMsg = err?.error?.message;
        const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
        this.error = backendMsg || plainMsg || err?.message || 'Failed to activate panic';
        this.cdr.detectChanges();
      }
    });
  }

  private handleRideCompletion(rideId: number): void {
    this.stopTracking();
    this.isRideActive = false;
    this.form.enable();
    this.completedRideId = rideId;
    this.showRatingModal = true;

    if (this.orderResult) {
      this.orderResult.status = 'COMPLETED';
      this.orderResult.message = 'Ride completed successfully!';
    }

    this.cdr.detectChanges();
  }

  setVehicleRating(rating: number): void {
    this.vehicleRating = rating;
  }

  setDriverRating(rating: number): void {
    this.driverRating = rating;
  }

  submitRating(): void {
    if (!this.completedRideId || this.vehicleRating === 0 || this.driverRating === 0) {
      this.error = 'Please provide both vehicle and driver ratings';
      return;
    }

    this.ratingSubmitting = true;
    this.error = undefined;

    this.rides.rateRide(this.completedRideId, {
      vehicleRating: this.vehicleRating,
      driverRating: this.driverRating,
      comment: this.ratingComment.trim() || undefined
    }).subscribe({
      next: (response) => {
        this.ratingSubmitting = false;
        this.ratingSuccess = response.message || 'Rating submitted successfully!';
        setTimeout(() => {
          this.closeRatingModal();
        }, 2000);
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.ratingSubmitting = false;
        this.error = err?.error?.message || 'Failed to submit rating';
        this.cdr.detectChanges();
      }
    });
  }

  closeRatingModal(): void {
    this.showRatingModal = false;
    this.vehicleRating = 0;
    this.driverRating = 0;
    this.ratingComment = '';
    this.ratingSuccess = undefined;
    this.completedRideId = undefined;
    this.orderResult = undefined;
    this.cdr.detectChanges();
  }

  skipRating(): void {
    this.closeRatingModal();
  }

  isCoordinates(address: string): boolean {
    if (!address) return false;
    // Check if address looks like coordinates (e.g., "45.262373, 19.839226" or "45.262373,19.839226")
    const coordsPattern = /^-?\d+\.?\d*,?\s*-?\d+\.?\d*$/;
    return coordsPattern.test(address.trim());
  }
}
