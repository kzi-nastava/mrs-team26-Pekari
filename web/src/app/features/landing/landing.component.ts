import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, inject, AfterViewInit, OnDestroy, ElementRef, ViewChild, QueryList, ViewChildren, ChangeDetectorRef } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { RideApiService, RideEstimateResponse, LocationPoint } from '../../core/services/ride-api.service';
import { EnvironmentService } from '../../core/services/environment.service';
import { GeocodingService } from '../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../shared/components/address-autocomplete/address-autocomplete.component';
import * as L from 'leaflet';

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
  imports: [CommonModule, ReactiveFormsModule, AddressAutocompleteComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: false }) mapContainer?: ElementRef;
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private geocoding = inject(GeocodingService);
  private http = inject(HttpClient);
  private env = inject(EnvironmentService);
  private cdr = inject(ChangeDetectorRef);

  private map?: L.Map;
  private pickupMarker?: L.Marker;
  private dropoffMarker?: L.Marker;
  private stopMarkers: L.Marker[] = [];
  private routeLine?: L.Polyline;
  private driverMarkers: L.Marker[] = [];
  private valueChangesSubscription?: Subscription;

  private focusedInput: FocusedInput = null;

  estimate?: RideEstimateResponse;
  error?: string;

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

  addStop() {
    this.stops.push(
      this.fb.group({
        address: ['', Validators.required],
        latitude: [null as number | null, Validators.required],
        longitude: [null as number | null, Validators.required]
      })
    );
  }

  removeStop(index: number) {
    this.stops.removeAt(index);
    if (index < this.stopMarkers.length) {
      this.stopMarkers[index].remove();
      this.stopMarkers.splice(index, 1);
    }
  }

  clearPickup() {
    this.form.patchValue({
      pickup: { address: '', latitude: null, longitude: null }
    });
    if (this.pickupAutocomplete) {
      this.pickupAutocomplete.setAddress('');
    }
    if (this.pickupMarker) {
      this.pickupMarker.remove();
      this.pickupMarker = undefined;
    }
  }

  clearDropoff() {
    this.form.patchValue({
      dropoff: { address: '', latitude: null, longitude: null }
    });
    if (this.dropoffAutocomplete) {
      this.dropoffAutocomplete.setAddress('');
    }
    if (this.dropoffMarker) {
      this.dropoffMarker.remove();
      this.dropoffMarker = undefined;
    }
  }

  ngAfterViewInit(): void {
    this.initializeMap();
    this.valueChangesSubscription = this.form.valueChanges.subscribe(() => {
      this.updateMapMarkers();
      this.clearEstimateAndRoute();
    });
  }

  ngOnDestroy(): void {
    this.valueChangesSubscription?.unsubscribe();
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

  private initializeMap(): void {
    if (!this.mapContainer) return;

    this.map = L.map(this.mapContainer.nativeElement, {
      center: [45.2671, 19.8335],
      zoom: 13
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => this.onMapClick(e));
    this.loadOnlineDrivers();
  }

  private updateMapMarkers(): void {
    if (!this.map) return;

    const value = this.form.getRawValue();

    if (value.pickup?.latitude && value.pickup?.longitude) {
      if (this.pickupMarker) {
        this.pickupMarker.setLatLng([value.pickup.latitude, value.pickup.longitude]);
      } else {
        this.pickupMarker = L.marker([value.pickup.latitude, value.pickup.longitude], {
          icon: this.createCustomIcon('green')
        })
          .bindPopup('Pickup Location')
          .addTo(this.map);
      }
    }

    if (value.dropoff?.latitude && value.dropoff?.longitude) {
      if (this.dropoffMarker) {
        this.dropoffMarker.setLatLng([value.dropoff.latitude, value.dropoff.longitude]);
      } else {
        this.dropoffMarker = L.marker([value.dropoff.latitude, value.dropoff.longitude], {
          icon: this.createCustomIcon('red')
        })
          .bindPopup('Dropoff Location')
          .addTo(this.map);
      }
    }

    this.stopMarkers.forEach(marker => marker.remove());
    this.stopMarkers = [];

    const stops = (value.stops || []) as Array<{ address?: string, latitude?: number | null, longitude?: number | null }>;
    stops.forEach((stop, index) => {
      if (stop?.latitude && stop?.longitude) {
        const marker = L.marker([stop.latitude, stop.longitude], {
          icon: this.createCustomIcon('blue')
        })
          .bindPopup(`Stop ${index + 1}`)
          .addTo(this.map!);
        this.stopMarkers.push(marker);
      }
    });

    this.drawRoute();
  }

  private drawRoute(): void {
    if (!this.map) return;

    if (this.routeLine) {
      this.routeLine.remove();
    }

    const value = this.form.getRawValue();
    const points: L.LatLngExpression[] = [];

    if (value.pickup?.latitude && value.pickup?.longitude) {
      points.push([value.pickup.latitude, value.pickup.longitude]);
    }

    const stops = value.stops as any[] || [];
    stops.forEach(stop => {
      if (stop?.latitude && stop?.longitude) {
        points.push([stop.latitude, stop.longitude]);
      }
    });

    if (value.dropoff?.latitude && value.dropoff?.longitude) {
      points.push([value.dropoff.latitude, value.dropoff.longitude]);
    }

    if (points.length >= 2) {
      this.routeLine = L.polyline(points, {
        color: '#3b82f6',
        weight: 4,
        opacity: 0.7,
        dashArray: '10, 10'
      }).addTo(this.map);

      this.map.fitBounds(this.routeLine.getBounds(), { padding: [50, 50] });
    }
  }

  private createCustomIcon(color: 'green' | 'red' | 'blue'): L.DivIcon {
    const colorMap = {
      green: '#22c55e',
      red: '#ef4444',
      blue: '#3b82f6'
    };

    return L.divIcon({
      className: 'custom-marker',
      html: `<div style="background-color: ${colorMap[color]}; width: 24px; height: 24px; border-radius: 50%; border: 3px solid white; box-shadow: 0 2px 8px rgba(0,0,0,0.3);"></div>`,
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });
  }

  private loadOnlineDrivers(page = 0, size = 50): void {
    this.http
      .get<OnlineDriverVehicle[]>(
        `${this.env.getApiUrl()}/drivers/online-with-vehicles?page=${page}&size=${size}`
      )
      .subscribe({
        next: (drivers: OnlineDriverVehicle[] | null) => this.renderOnlineDrivers(drivers || []),
        error: () => {
          this.driverMarkers.forEach(marker => marker.remove());
          this.driverMarkers = [];
        }
      });
  }

  private renderOnlineDrivers(drivers: OnlineDriverVehicle[]): void {
    if (!this.map) return;

    this.driverMarkers.forEach(marker => marker.remove());
    this.driverMarkers = [];

    drivers
      .filter(driver => driver?.driverState?.online)
      .forEach(driver => {
        const state = driver.driverState;
        if (!Number.isFinite(state?.latitude) || !Number.isFinite(state?.longitude)) return;

        const marker = L.marker([state.latitude, state.longitude], {
          icon: this.createCustomIcon(state.busy ? 'red' : 'green')
        })
          .bindPopup(
            `<strong>${driver.vehicleRegistration}</strong><br/>${driver.vehicleType}<br/>${state.driverEmail}`
          )
          .addTo(this.map!);

        this.driverMarkers.push(marker);
      });
  }

  private onMapClick(e: L.LeafletMouseEvent): void {
    const lat = e.latlng.lat;
    const lng = e.latlng.lng;

    this.geocoding.reverseGeocode(lat, lng).subscribe({
      next: (result) => {
        if (result) {
          if (this.focusedInput === 'pickup') {
            this.updatePickupLocation(result.displayName, lat, lng);
          } else if (this.focusedInput === 'dropoff') {
            this.updateDropoffLocation(result.displayName, lat, lng);
          } else if (this.focusedInput && typeof this.focusedInput === 'object' && this.focusedInput.type === 'stop') {
            this.updateStopLocation(this.focusedInput.index, result.displayName, lat, lng);
          } else {
            const pickupAddress = this.form.get('pickup.address')?.value;
            const dropoffAddress = this.form.get('dropoff.address')?.value;

            if (!pickupAddress || pickupAddress.trim() === '') {
              this.updatePickupLocation(result.displayName, lat, lng);
            } else if (!dropoffAddress || dropoffAddress.trim() === '') {
              this.updateDropoffLocation(result.displayName, lat, lng);
            } else {
              this.updateDropoffLocation(result.displayName, lat, lng);
            }
          }
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
          console.log('Estimate response:', resp);
          this.estimate = resp;
          this.drawEstimatedRoute(resp.routePoints);
          this.cdr.markForCheck();
        },
        error: (err) => {
          console.error('Estimate error:', err);
          const backendMsg = err?.error?.message;
          const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
          this.error = backendMsg || plainMsg || err?.message || 'Estimate failed';
          this.cdr.markForCheck();
        }
      });
  }

  private clearRoute(): void {
    if (this.routeLine) {
      this.routeLine.remove();
      this.routeLine = undefined;
    }
  }

  private clearEstimateAndRoute(): void {
    this.estimate = undefined;
    this.error = undefined;
    this.clearRoute();
    this.cdr.markForCheck();
  }

  private drawEstimatedRoute(routePoints?: LocationPoint[]): void {
    if (!this.map || !routePoints || routePoints.length < 2) return;

    this.clearRoute();

    const points: L.LatLngExpression[] = routePoints.map(p => [p.latitude, p.longitude]);

    this.routeLine = L.polyline(points, {
      color: '#3b82f6',
      weight: 5,
      opacity: 0.8
    }).addTo(this.map);

    this.map.fitBounds(this.routeLine.getBounds(), { padding: [50, 50] });
  }
}
