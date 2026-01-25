import { Component, Input, Output, EventEmitter, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { Subject, Subscription } from 'rxjs';
import { GeocodingService, GeocodeResult } from '../../../core/services/geocoding.service';

export interface AddressSelection {
  address: string;
  latitude: number;
  longitude: number;
}

@Component({
  selector: 'app-address-autocomplete',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './address-autocomplete.component.html',
  styleUrl: './address-autocomplete.component.css'
})
export class AddressAutocompleteComponent implements OnInit, OnDestroy {
  @Input() placeholder = '';
  @Input() initialValue = '';
  @Output() addressSelected = new EventEmitter<AddressSelection>();
  @Output() focused = new EventEmitter<void>();

  addressControl = new FormControl('');
  searchQuery$ = new Subject<string>();
  suggestions: GeocodeResult[] = [];
  showSuggestions = false;

  private subscription?: Subscription;

  constructor(private geocoding: GeocodingService) {}

  ngOnInit(): void {
    if (this.initialValue) {
      this.addressControl.setValue(this.initialValue, { emitEvent: false });
    }

    this.subscription = this.geocoding
      .autocompleteSearch(this.searchQuery$)
      .subscribe(results => {
        this.suggestions = results;
        this.showSuggestions = results.length > 0;
      });

    this.addressControl.valueChanges.subscribe(value => {
      if (value && value.trim().length >= 3) {
        this.searchQuery$.next(value);
      } else {
        this.suggestions = [];
        this.showSuggestions = false;
      }
    });
  }

  ngOnDestroy(): void {
    this.subscription?.unsubscribe();
  }

  select(s: GeocodeResult): void {
    this.addressControl.setValue(s.displayName, { emitEvent: false });
    this.showSuggestions = false;
    this.suggestions = [];
    this.addressSelected.emit({
      address: s.displayName,
      latitude: s.latitude,
      longitude: s.longitude
    });
  }

  onFocus(): void {
    if (this.suggestions.length > 0) this.showSuggestions = true;
    this.focused.emit();
  }

  onBlur(): void {
    setTimeout(() => this.showSuggestions = false, 200);
  }

  setAddress(address: string): void {
    this.addressControl.setValue(address, { emitEvent: false });
    this.showSuggestions = false;
    this.suggestions = [];
  }
}
