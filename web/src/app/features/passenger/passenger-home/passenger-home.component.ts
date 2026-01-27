import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, ChangeDetectorRef, ViewChild, ViewChildren, QueryList } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, OrderRideResponse, RideEstimateResponse, LocationPoint } from '../../../core/services/ride-api.service';
import { GeocodingService } from '../../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../../shared/components/address-autocomplete/address-autocomplete.component';
import { RideMapComponent } from '../../../shared/components/ride-map/ride-map.component';

type FocusedInput = 'pickup' | 'dropoff' | { type: 'stop', index: number } | null;

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
  private geocoding = inject(GeocodingService);
  private cdr = inject(ChangeDetectorRef);

  private focusedInput: FocusedInput = null;

  estimate?: RideEstimateResponse;
  orderResult?: OrderRideResponse;
  error?: string;
  isRideActive = false;

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

    // Convenience: show current time in the picker, but don't actually schedule unless user changes it.
    this.form.patchValue({ scheduledAt: this.scheduledMin }, { emitEvent: false });

    // Subscribe to form changes
    this.form.valueChanges.subscribe(() => {
      this.estimate = undefined;
      this.error = undefined;
    });
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

  chooseFavoriteRoute() {
    this.error = 'Favorite routes are not implemented yet.';
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
            this.form.disable(); // Disable form when ride is active
          }
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

  resetForm(): void {
    this.isRideActive = false;
    this.form.enable();
    this.clearMessages();
    this.cdr.detectChanges();
  }
}
