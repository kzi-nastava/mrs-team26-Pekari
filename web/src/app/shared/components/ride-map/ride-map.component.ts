import { Component, ElementRef, ViewChild, AfterViewInit, OnDestroy, Input, Output, EventEmitter, OnChanges, SimpleChanges, ChangeDetectionStrategy } from '@angular/core';
import { CommonModule } from '@angular/common';
import * as L from 'leaflet';
import { LocationPoint } from '../../../core/services/ride-api.service';

export interface MapMarker {
  latitude: number;
  longitude: number;
  type: 'pickup' | 'dropoff' | 'stop' | 'driver';
  label?: string;
  popupContent?: string;
  busy?: boolean;
}

@Component({
  selector: 'app-ride-map',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './ride-map.component.html',
  styleUrl: './ride-map.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RideMapComponent implements AfterViewInit, OnDestroy, OnChanges {
  @ViewChild('mapContainer', { static: false }) mapContainer?: ElementRef;

  @Input() pickupLocation?: { latitude: number; longitude: number } | null;
  @Input() dropoffLocation?: { latitude: number; longitude: number } | null;
  @Input() stops: Array<{ latitude: number; longitude: number; label?: string }> = [];
  @Input() drivers: Array<{ latitude: number; longitude: number; busy?: boolean; popupContent: string }> = [];
  @Input() driverLocation?: { latitude: number; longitude: number } | null;
  @Input() routePoints: Array<{ latitude: number; longitude: number }> = [];
  @Input() estimatedRoute?: LocationPoint[];
  @Input() locked = false;
  @Input() center: [number, number] = [45.2671, 19.8335];
  @Input() zoom = 13;

  @Output() mapClick = new EventEmitter<{ latitude: number; longitude: number }>();

  private map?: L.Map;
  private pickupMarker?: L.Marker;
  private dropoffMarker?: L.Marker;
  private stopMarkers: L.Marker[] = [];
  private driverMarkers: L.Marker[] = [];
  private trackingDriverMarker?: L.Marker;
  private routeLine?: L.Polyline;

  // Cache for icons to avoid recreating them
  private iconCache = new Map<string, L.DivIcon>();

  ngAfterViewInit(): void {
    // Delay map initialization slightly to ensure container is ready
    setTimeout(() => this.initializeMap(), 0);
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (!this.map) return;

    // Batch updates to avoid multiple redraws
    const updateTasks: Array<() => void> = [];

    if (changes['pickupLocation'] && !this.isSameLocation(changes['pickupLocation'].previousValue, changes['pickupLocation'].currentValue)) {
      updateTasks.push(() => this.updatePickupMarker());
    }

    if (changes['dropoffLocation'] && !this.isSameLocation(changes['dropoffLocation'].previousValue, changes['dropoffLocation'].currentValue)) {
      updateTasks.push(() => this.updateDropoffMarker());
    }

    if (changes['stops']) {
      updateTasks.push(() => this.updateStopMarkers());
    }

    if (changes['drivers']) {
      updateTasks.push(() => this.updateDriverMarkers());
    }

    if (changes['driverLocation'] && !this.isSameLocation(changes['driverLocation'].previousValue, changes['driverLocation'].currentValue)) {
      updateTasks.push(() => this.updateTrackingDriverMarker());
    }

    if (changes['routePoints'] || changes['estimatedRoute']) {
      updateTasks.push(() => this.updateRoutes());
    }

    if (changes['locked']) {
      updateTasks.push(() => this.updateMapLock());
    }

    // Execute all updates
    if (updateTasks.length > 0) {
      requestAnimationFrame(() => {
        updateTasks.forEach(task => task());
      });
    }
  }

  ngOnDestroy(): void {
    this.iconCache.clear();
    if (this.map) {
      this.map.remove();
      this.map = undefined;
    }
  }

  private initializeMap(): void {
    if (!this.mapContainer) return;

    this.map = L.map(this.mapContainer.nativeElement, {
      center: this.center,
      zoom: this.zoom,
      zoomControl: true,
      preferCanvas: true // Better performance for many markers
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(this.map);

    this.map.on('click', (e: L.LeafletMouseEvent) => {
      if (!this.locked) {
        this.mapClick.emit({ latitude: e.latlng.lat, longitude: e.latlng.lng });
      }
    });

    // Initialize all markers
    this.updatePickupMarker();
    this.updateDropoffMarker();
    this.updateStopMarkers();
    this.updateDriverMarkers();
    this.updateTrackingDriverMarker();
    this.updateRoutes();
  }

  private isSameLocation(loc1: any, loc2: any): boolean {
    if (!loc1 && !loc2) return true;
    if (!loc1 || !loc2) return false;
    return loc1.latitude === loc2.latitude && loc1.longitude === loc2.longitude;
  }

  private updatePickupMarker(): void {
    if (!this.map) return;

    if (this.pickupLocation) {
      if (this.pickupMarker) {
        // Update existing marker position instead of recreating
        this.pickupMarker.setLatLng([this.pickupLocation.latitude, this.pickupLocation.longitude]);
      } else {
        this.pickupMarker = L.marker([this.pickupLocation.latitude, this.pickupLocation.longitude], {
          icon: this.getOrCreateIcon('location-green')
        })
          .bindPopup('Pickup Location')
          .addTo(this.map);
      }
    } else if (this.pickupMarker) {
      this.pickupMarker.remove();
      this.pickupMarker = undefined;
    }
  }

  private updateDropoffMarker(): void {
    if (!this.map) return;

    if (this.dropoffLocation) {
      if (this.dropoffMarker) {
        // Update existing marker position instead of recreating
        this.dropoffMarker.setLatLng([this.dropoffLocation.latitude, this.dropoffLocation.longitude]);
      } else {
        this.dropoffMarker = L.marker([this.dropoffLocation.latitude, this.dropoffLocation.longitude], {
          icon: this.getOrCreateIcon('location-red')
        })
          .bindPopup('Dropoff Location')
          .addTo(this.map);
      }
    } else if (this.dropoffMarker) {
      this.dropoffMarker.remove();
      this.dropoffMarker = undefined;
    }
  }

  private updateStopMarkers(): void {
    if (!this.map) return;

    // Remove excess markers
    while (this.stopMarkers.length > this.stops.length) {
      const marker = this.stopMarkers.pop();
      marker?.remove();
    }

    // Update or create markers
    this.stops.forEach((stop, index) => {
      const label = stop.label || `Stop ${index + 1}`;

      if (this.stopMarkers[index]) {
        // Update existing marker
        this.stopMarkers[index].setLatLng([stop.latitude, stop.longitude]);
        this.stopMarkers[index].setPopupContent(label);
      } else {
        // Create new marker
        const marker = L.marker([stop.latitude, stop.longitude], {
          icon: this.getOrCreateIcon('location-blue')
        })
          .bindPopup(label)
          .addTo(this.map!);
        this.stopMarkers.push(marker);
      }
    });
  }

  private updateDriverMarkers(): void {
    if (!this.map) return;

    // Remove all existing driver markers
    this.driverMarkers.forEach(marker => marker.remove());
    this.driverMarkers = [];

    // Add new driver markers
    this.drivers.forEach(driver => {
      const iconKey = driver.busy ? 'car-red' : 'car-green';
      const marker = L.marker([driver.latitude, driver.longitude], {
        icon: this.getOrCreateIcon(iconKey)
      })
        .bindPopup(driver.popupContent)
        .addTo(this.map!);
      this.driverMarkers.push(marker);
    });
  }

  private updateTrackingDriverMarker(): void {
    if (!this.map) return;

    if (this.driverLocation) {
      if (this.trackingDriverMarker) {
        // Smoothly update existing marker position
        this.trackingDriverMarker.setLatLng([this.driverLocation.latitude, this.driverLocation.longitude]);
      } else {
        this.trackingDriverMarker = L.marker([this.driverLocation.latitude, this.driverLocation.longitude], {
          icon: this.getOrCreateIcon('car-green')
        })
          .bindPopup('Your Driver')
          .addTo(this.map);
      }
    } else if (this.trackingDriverMarker) {
      this.trackingDriverMarker.remove();
      this.trackingDriverMarker = undefined;
    }
  }

  private updateRoutes(): void {
    if (!this.map) return;

    // Clear existing route
    if (this.routeLine) {
      this.routeLine.remove();
      this.routeLine = undefined;
    }

    // Priority: estimatedRoute > routePoints
    if (this.estimatedRoute && this.estimatedRoute.length >= 2) {
      this.drawRoute(this.estimatedRoute, false); // Solid line for estimated route
    } else if (this.routePoints.length >= 2) {
      // Don't draw dashed line - only show markers
      // User will see straight connection through markers visually
    }
  }

  private drawRoute(points: Array<{ latitude: number; longitude: number }>, dashed: boolean): void {
    if (!this.map || points.length < 2) return;

    const latLngPoints: L.LatLngExpression[] = points.map(p => [p.latitude, p.longitude]);

    const options: L.PolylineOptions = {
      color: '#3b82f6',
      weight: 5,
      opacity: 0.8
    };

    if (dashed) {
      options.dashArray = '10, 10';
    }

    this.routeLine = L.polyline(latLngPoints, options).addTo(this.map);

    // Fit bounds only if route is long enough
    const bounds = this.routeLine.getBounds();
    if (bounds.isValid()) {
      this.map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
    }
  }

  private updateMapLock(): void {
    if (!this.map) return;

    if (this.locked) {
      this.map.dragging.disable();
      this.map.touchZoom.disable();
      this.map.doubleClickZoom.disable();
      this.map.scrollWheelZoom.disable();
      this.map.boxZoom.disable();
      this.map.keyboard.disable();
    } else {
      this.map.dragging.enable();
      this.map.touchZoom.enable();
      this.map.doubleClickZoom.enable();
      this.map.scrollWheelZoom.enable();
      this.map.boxZoom.enable();
      this.map.keyboard.enable();
    }
  }

  private getOrCreateIcon(key: string): L.DivIcon {
    if (!this.iconCache.has(key)) {
      let icon: L.DivIcon;

      if (key.startsWith('location-')) {
        const color = key.split('-')[1] as 'green' | 'red' | 'blue';
        icon = this.createLocationIcon(color);
      } else if (key.startsWith('car-')) {
        const color = key.split('-')[1] as 'green' | 'red';
        icon = this.createCarIcon(color);
      } else {
        icon = this.createLocationIcon('blue');
      }

      this.iconCache.set(key, icon);
    }

    return this.iconCache.get(key)!;
  }

  private createLocationIcon(color: 'green' | 'red' | 'blue'): L.DivIcon {
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

  private createCarIcon(color: 'green' | 'red'): L.DivIcon {
    const bgColor = color === 'green' ? '#22c55e' : '#ef4444';

    return L.divIcon({
      className: 'custom-car-marker',
      html: `
        <div style="
          background-color: ${bgColor};
          width: 32px;
          height: 32px;
          border-radius: 8px;
          border: 3px solid white;
          box-shadow: 0 2px 8px rgba(0,0,0,0.3);
          display: flex;
          align-items: center;
          justify-content: center;
        ">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="white">
            <path d="M18.92 6.01C18.72 5.42 18.16 5 17.5 5h-11c-.66 0-1.21.42-1.42 1.01L3 12v8c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-1h12v1c0 .55.45 1 1 1h1c.55 0 1-.45 1-1v-8l-2.08-5.99zM6.5 16c-.83 0-1.5-.67-1.5-1.5S5.67 13 6.5 13s1.5.67 1.5 1.5S7.33 16 6.5 16zm11 0c-.83 0-1.5-.67-1.5-1.5s.67-1.5 1.5-1.5 1.5.67 1.5 1.5-.67 1.5-1.5 1.5zM5 11l1.5-4.5h11L19 11H5z"/>
          </svg>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 16]
    });
  }

  clearRoute(): void {
    if (this.routeLine) {
      this.routeLine.remove();
      this.routeLine = undefined;
    }
  }
}
