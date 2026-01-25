import { CommonModule } from '@angular/common';
import { Component, inject, AfterViewInit, ElementRef, ViewChild, QueryList, ViewChildren } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RideApiService, RideEstimateResponse } from '../../core/services/ride-api.service';
import { GeocodingService } from '../../core/services/geocoding.service';
import { AddressAutocompleteComponent, AddressSelection } from '../../shared/components/address-autocomplete/address-autocomplete.component';
import * as L from 'leaflet';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, AddressAutocompleteComponent],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.css'
})
export class LandingComponent implements AfterViewInit {
  @ViewChild('mapContainer', { static: false }) mapContainer?: ElementRef;
  @ViewChild('pickupAutocomplete') pickupAutocomplete?: AddressAutocompleteComponent;
  @ViewChild('dropoffAutocomplete') dropoffAutocomplete?: AddressAutocompleteComponent;
  @ViewChildren('stopAutocomplete') stopAutocompletes?: QueryList<AddressAutocompleteComponent>;

  private fb = inject(FormBuilder);
  private rides = inject(RideApiService);
  private geocoding = inject(GeocodingService);

  private map?: L.Map;
  private pickupMarker?: L.Marker;
  private dropoffMarker?: L.Marker;
  private stopMarkers: L.Marker[] = [];
  private routeLine?: L.Polyline;

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
    vehicleType: ['STANDARD', Validators.required],
    babyTransport: [false],
    petTransport: [false]
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
    this.drawRoute();
  }

  ngAfterViewInit(): void {
    this.initializeMap();
    this.form.valueChanges.subscribe(() => this.updateMapMarkers());
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

  private onMapClick(e: L.LeafletMouseEvent): void {
    const lat = e.latlng.lat;
    const lng = e.latlng.lng;

    this.geocoding.reverseGeocode(lat, lng).subscribe({
      next: (result) => {
        if (result) {
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
        },
        error: (err) => {
          const backendMsg = err?.error?.message;
          const plainMsg = typeof err?.error === 'string' ? err.error : undefined;
          this.error = backendMsg || plainMsg || err?.message || 'Estimate failed';
        }
      });
  }
}
