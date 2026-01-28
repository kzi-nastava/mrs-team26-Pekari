import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

export interface GeocodeResult {
  displayName: string;
  latitude: number;
  longitude: number;
  placeId: number;
}

@Injectable({ providedIn: 'root' })
export class GeocodingService {
  constructor(private http: HttpClient) {}

  searchAddress(query: string): Observable<GeocodeResult[]> {
    if (!query || query.trim().length < 3) return of([]);

    return this.http
      .get<any>('/nominatim/search', {
        params: {
          q: query,
          format: 'json',
          addressdetails: '1',
          limit: '8',
          countrycodes: 'rs',
          viewbox: '19.7,45.3,19.9,45.2',
          bounded: '1'
        }
      })
      .pipe(
        map(res => res.map((r: any) => {
          const parts = [];
          if (r.address?.road) {
            parts.push(r.address.road);
            if (r.address?.house_number) {
              parts[0] += ' ' + r.address.house_number;
            }
          }
          if (r.address?.city || r.address?.town || r.address?.village) {
            parts.push(r.address.city || r.address.town || r.address.village);
          }
          if (r.address?.country) {
            parts.push(r.address.country);
          }

          return {
            displayName: parts.length > 0 ? parts.join(', ') : r.display_name,
            latitude: parseFloat(r.lat),
            longitude: parseFloat(r.lon),
            placeId: r.place_id
          };
        })),
        catchError(() => of([]))
      );
  }

  reverseGeocode(latitude: number, longitude: number): Observable<GeocodeResult | null> {
    return this.http
      .get<any>('/nominatim/reverse', {
        params: { lat: latitude.toString(), lon: longitude.toString(), format: 'json' }
      })
      .pipe(
        map(r => r?.display_name ? {
          displayName: r.display_name,
          latitude: parseFloat(r.lat),
          longitude: parseFloat(r.lon),
          placeId: r.place_id || 0
        } : null),
        catchError(() => of({
          displayName: `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
          latitude,
          longitude,
          placeId: 0
        }))
      );
  }

  autocompleteSearch(query$: Observable<string>): Observable<GeocodeResult[]> {
    return query$.pipe(
      debounceTime(200),
      distinctUntilChanged(),
      switchMap(q => this.searchAddress(q))
    );
  }
}
