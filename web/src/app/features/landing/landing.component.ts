import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, OnInit, OnDestroy, ViewChild, QueryList, ViewChildren, ChangeDetectorRef } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, RideEstimateResponse, LocationPoint } from '../../core/services/ride-api.service';
import { EnvironmentService } from '../../core/services/environment.service';
import { GeocodingService } from '../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../shared/components/address-autocomplete/address-autocomplete.component';
import { RideMapComponent } from '../../shared/components/ride-map/ride-map.component';

interface DriverState {
  driverId: number;
  driverEmail: string;
  online: boolean;
  busy: boolean;
  latitude: number;
  longitude: number;
  updatedAt: string;
}

interface OnlineDriverVehicle {
  driverState: DriverState;
  vehicleRegistration: string;
  vehicleType: string;
}

type FocusedInput = 'pickup' | 'dropoff' | { type: 'stop', index: number } | null;

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AddressAutocompleteComponent, RideMapComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements OnInit, OnDestroy {
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private geocoding = inject(GeocodingService);
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private cdr = inject(ChangeDetectorRef);

  private focusedInput: FocusedInput = null;

  estimate?: RideEstimateResponse;
  error?: string;

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

  get mapDrivers() {
    return this.onlineDrivers;
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

  onlineDrivers: Array<{ latitude: number; longitude: number; busy?: boolean; popupContent: string }> = [];

  form = this.fb.group({
    pickup: this.fb.group({
      address: ['', Validators.required],
      latitude: [null as number | null, Validators.required],
      longitude: [null as number | null, Validators.required]
    }),
    stops: this.fb.array([]),
    dropoff: this.fb.group({
      address: ['', Validators.required],
      latitude: [null as number | null, Validators.required],
      longitude: [null as number | null, Validators.required]
    }),
    vehicleType: ['STANDARD', Validators.required]
  });

  get stops(): FormArray {
    return this.form.get('stops') as FormArray;
  }

  ngOnInit(): void {
    this.loadOnlineDrivers();

    this.form.valueChanges.subscribe(() => {
      this.estimate = undefined;
      this.error = undefined;
    });
  }

  ngOnDestroy(): void {
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

  onPickupFocus(): void {
    this.focusedInput = 'pickup';
  }

  onDropoffFocus(): void {
    this.focusedInput = 'dropoff';
  }

  onStopFocus(index: number): void {
    this.focusedInput = { type: 'stop', index };
  }

  private loadOnlineDrivers(page = 0, size = 50): void {
    this.http
      .get<OnlineDriverVehicle[]>(
        `${this.env.getApiUrl()}/drivers/online-with-vehicles?page=${page}&size=${size}`
      )
      .subscribe({
        next: (drivers: OnlineDriverVehicle[] | null) => {
          this.onlineDrivers = (drivers || [])
            .filter(driver => driver?.driverState?.online)
            .filter(driver => {
              const state = driver.driverState;
              return Number.isFinite(state?.latitude) && Number.isFinite(state?.longitude);
            })
            .map(driver => {
              const state = driver.driverState;
              return {
                latitude: state.latitude,
                longitude: state.longitude,
                busy: state.busy,
                popupContent: `<strong>${driver.vehicleRegistration}</strong><br/>${driver.vehicleType}<br/>${state.driverEmail}`
              };
            });
          this.cdr.detectChanges();
        },
        error: () => {
          this.onlineDrivers = [];
          this.cdr.detectChanges();
        }
      });
  }

  onMapClick(event: { latitude: number; longitude: number }): void {
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

      },
      error: () => {
        // On error, use coordinates as address
        const fallbackAddress = `${event.latitude.toFixed(6)}, ${event.longitude.toFixed(6)}`;

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

  setVehicleType(vehicleType: 'STANDARD' | 'VAN' | 'LUX') {
    this.form.patchValue({ vehicleType });
  }

  estimateRide() {
    this.error = undefined;
    this.estimate = undefined;

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
}
