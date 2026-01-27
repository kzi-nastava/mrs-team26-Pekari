import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, AfterViewInit, inject, ChangeDetectorRef, ElementRef, ViewChild, ViewChildren, QueryList } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, OrderRideResponse, RideEstimateResponse, LocationPoint, FavoriteRoute } from '../../../core/services/ride-api.service';
import { GeocodingService } from '../../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../../shared/components/address-autocomplete/address-autocomplete.component';
import * as L from 'leaflet';

type FocusedInput = 'pickup' | 'dropoff' | { type: 'stop', index: number } | null;

@Component({
  selector: 'app-passenger-home',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AddressAutocompleteComponent],
  templateUrl: './passenger-home.component.html',
  styleUrl: './passenger-home.component.css'
})
export class PassengerHomeComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('mapContainer', { static: false }) mapContainer?: ElementRef;
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private geocoding = inject(GeocodingService);
  private cdr = inject(ChangeDetectorRef);

  private map?: L.Map;
  private pickupMarker?: L.Marker;
  private dropoffMarker?: L.Marker;
  private stopMarkers: L.Marker[] = [];
  private routeLine?: L.Polyline;
  private focusedInput: FocusedInput = null;

  estimate?: RideEstimateResponse;
  orderResult?: OrderRideResponse;
  error?: string;
  favoriteRoutes: FavoriteRoute[] = [];
  showFavoriteRoutesModal = false;

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

    // Subscribe to form changes to update map
    this.form.valueChanges.subscribe(() => {
      this.updateMapMarkers();
      this.clearEstimateAndRoute();
    });

    // Load favorite routes
    this.loadFavoriteRoutes();
  }

  ngAfterViewInit(): void {
    this.initializeMap();
  }

  ngOnDestroy(): void {
    if (this.scheduleBoundsTimer) {
      window.clearInterval(this.scheduleBoundsTimer);
    }
    if (this.map) {
      this.map.remove();
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

    this.updateMapMarkers();
    this.cdr.detectChanges();
  }

  closeFavoriteRoutesModal() {
    this.showFavoriteRoutesModal = false;
    this.cdr.detectChanges();
  }

  private initializeMap(): void {
    if (!this.mapContainer) return;

    // Initialize map centered on Novi Sad
    this.map = L.map(this.mapContainer.nativeElement, {
      center: [45.2671, 19.8335],
      zoom: 13,
      zoomControl: true
    });

    // Add OpenStreetMap tile layer
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(this.map);

    // Initialize markers based on form values
    this.updateMapMarkers();

    // Add click listener to map
    this.map.on('click', (e: L.LeafletMouseEvent) => {
      this.onMapClick(e);
    });
  }

  private updateMapMarkers(): void {
    if (!this.map) return;

    const value = this.form.getRawValue();

    // Update pickup marker
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

    // Update dropoff marker
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

    // Clear existing stop markers
    this.stopMarkers.forEach(marker => marker.remove());
    this.stopMarkers = [];

    // Add stop markers
    const stops = value.stops as any[] || [];
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

    // Draw route
    this.drawRoute();
  }

  private drawRoute(): void {
    if (!this.map) return;

    // Remove existing route
    if (this.routeLine) {
      this.routeLine.remove();
    }

    const value = this.form.getRawValue();
    const points: L.LatLngExpression[] = [];

    // Add pickup
    if (value.pickup?.latitude && value.pickup?.longitude) {
      points.push([value.pickup.latitude, value.pickup.longitude]);
    }

    // Add stops
    const stops = value.stops as any[] || [];
    stops.forEach(stop => {
      if (stop?.latitude && stop?.longitude) {
        points.push([stop.latitude, stop.longitude]);
      }
    });

    // Add dropoff
    if (value.dropoff?.latitude && value.dropoff?.longitude) {
      points.push([value.dropoff.latitude, value.dropoff.longitude]);
    }

    // Draw polyline if we have at least 2 points
    if (points.length >= 2) {
      this.routeLine = L.polyline(points, {
        color: '#3b82f6',
        weight: 4,
        opacity: 0.7,
        dashArray: '10, 10'
      }).addTo(this.map);

      // Fit map to show entire route
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

  private onMapClick(e: L.LeafletMouseEvent): void {
    const lat = e.latlng.lat;
    const lng = e.latlng.lng;

    this.geocoding.reverseGeocode(lat, lng).subscribe({
      next: (result) => {
        if (!result) return;

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
    this.error = undefined;
    this.orderResult = undefined;
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
          this.drawEstimatedRoute(resp.routePoints);
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
    this.cdr.detectChanges();
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
