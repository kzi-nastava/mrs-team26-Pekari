import { Component, OnInit, inject, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RideApiService, AdminRideHistoryResponse, AdminRideHistoryFilter } from '../../../core/services/ride-api.service';
import { AdminRideDetailModalComponent } from './admin-ride-detail-modal.component';

type SortField = 'createdAt' | 'startedAt' | 'completedAt' | 'price' | 'distanceKm' | 'status' | 'pickupAddress' | 'dropoffAddress';
type SortDirection = 'asc' | 'desc';

@Component({
  selector: 'app-admin-rides',
  standalone: true,
  imports: [CommonModule, FormsModule, AdminRideDetailModalComponent],
  templateUrl: './admin-rides.component.html',
  styleUrls: ['./admin-rides.component.css']
})
export class AdminRidesComponent implements OnInit {
  private rideService = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);

  ridesList: AdminRideHistoryResponse[] = [];
  displayedRides: AdminRideHistoryResponse[] = [];
  loading = false;
  error?: string;

  // Modal
  selectedRideId?: number;
  showModal = false;

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;

  // Date filter
  startDate: string = '';
  endDate: string = new Date().toISOString().split('T')[0];

  // Sorting
  sortField: SortField = 'createdAt';
  sortDirection: SortDirection = 'desc';

  ngOnInit(): void {
    // Set default start date to 1 year ago
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    this.startDate = oneYearAgo.toISOString().split('T')[0];

    this.loadRides();
  }

  loadRides() {
    this.loading = true;
    this.error = undefined;

    const filter: AdminRideHistoryFilter = {};
    if (this.startDate) {
      filter.startDate = this.startDate + 'T00:00:00';
    }
    if (this.endDate) {
      filter.endDate = this.endDate + 'T23:59:59';
    }

    this.rideService.getAdminRideHistory(filter, this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.ridesList = response.content;
        this.totalElements = response.totalElements;
        this.totalPages = Math.ceil(response.totalElements / this.pageSize);
        this.applySorting();
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.message || 'Failed to load ride history';
        this.loading = false;
        console.error('Error loading rides:', err);
        this.cdr.detectChanges();
      }
    });
  }

  applySorting() {
    this.displayedRides = [...this.ridesList].sort((a, b) => {
      let aVal: any;
      let bVal: any;

      switch (this.sortField) {
        case 'createdAt':
        case 'startedAt':
        case 'completedAt':
          aVal = a[this.sortField] ? new Date(a[this.sortField]!).getTime() : 0;
          bVal = b[this.sortField] ? new Date(b[this.sortField]!).getTime() : 0;
          break;
        case 'price':
        case 'distanceKm':
          aVal = a[this.sortField] || 0;
          bVal = b[this.sortField] || 0;
          break;
        case 'status':
        case 'pickupAddress':
        case 'dropoffAddress':
          aVal = (a[this.sortField] || '').toLowerCase();
          bVal = (b[this.sortField] || '').toLowerCase();
          break;
        default:
          aVal = a[this.sortField];
          bVal = b[this.sortField];
      }

      if (aVal < bVal) return this.sortDirection === 'asc' ? -1 : 1;
      if (aVal > bVal) return this.sortDirection === 'asc' ? 1 : -1;
      return 0;
    });
  }

  sortBy(field: SortField) {
    if (this.sortField === field) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    this.applySorting();
  }

  getSortIcon(field: SortField): string {
    if (this.sortField !== field) return '';
    return this.sortDirection === 'asc' ? ' ↑' : ' ↓';
  }

  applyFilter() {
    // Validate that startDate is not greater than endDate
    if (this.startDate && this.endDate && this.startDate > this.endDate) {
      this.error = 'Start date cannot be greater than end date';
      return;
    }
    this.currentPage = 0;
    this.loadRides();
  }

  isFilterValid(): boolean {
    if (!this.startDate || !this.endDate) return true;
    return this.startDate <= this.endDate;
  }

  resetFilter() {
    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);
    this.startDate = oneYearAgo.toISOString().split('T')[0];
    this.endDate = new Date().toISOString().split('T')[0];
    this.currentPage = 0;
    this.loadRides();
  }

  // Pagination
  goToPage(page: number) {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadRides();
    }
  }

  previousPage() {
    this.goToPage(this.currentPage - 1);
  }

  nextPage() {
    this.goToPage(this.currentPage + 1);
  }

  // Formatting helpers
  formatDateTime(dateTime: string | null): string {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    return date.toLocaleString('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatDate(dateTime: string | null): string {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    return date.toLocaleDateString('en-GB', {
      day: 'numeric',
      month: 'short',
      year: 'numeric'
    });
  }

  formatTime(dateTime: string | null): string {
    if (!dateTime) return '-';
    const date = new Date(dateTime);
    return date.toLocaleTimeString('en-GB', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  formatPrice(price: number): string {
    return price ? `${price.toLocaleString()} RSD` : '-';
  }

  formatDistance(km: number): string {
    return km ? `${km.toFixed(1)} km` : '-';
  }

  getDriverName(ride: AdminRideHistoryResponse): string {
    if (!ride.driver) return 'No driver';
    return `${ride.driver.firstName} ${ride.driver.lastName}`;
  }

  getPassengerNames(ride: AdminRideHistoryResponse): string {
    if (!ride.passengers || ride.passengers.length === 0) return 'No passengers';
    return ride.passengers.map(p => `${p.firstName} ${p.lastName}`).join(', ');
  }

  getStatusClass(ride: AdminRideHistoryResponse): string {
    if (ride.panicActivated) return 'status-panic';
    if (ride.cancelled) return 'status-cancelled';
    switch (ride.status) {
      case 'COMPLETED': return 'status-completed';
      case 'IN_PROGRESS': return 'status-in-progress';
      case 'ACCEPTED':
      case 'SCHEDULED': return 'status-scheduled';
      default: return '';
    }
  }

  getStatusLabel(ride: AdminRideHistoryResponse): string {
    if (ride.panicActivated) return 'PANIC';
    if (ride.cancelled) return `Cancelled (${ride.cancelledBy || 'unknown'})`;
    switch (ride.status) {
      case 'COMPLETED': return 'Completed';
      case 'IN_PROGRESS': return 'In Progress';
      case 'ACCEPTED': return 'Accepted';
      case 'SCHEDULED': return 'Scheduled';
      case 'PENDING': return 'Pending';
      default: return ride.status;
    }
  }

  onRideClick(ride: AdminRideHistoryResponse) {
    this.selectedRideId = ride.id;
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
    this.selectedRideId = undefined;
  }
}
