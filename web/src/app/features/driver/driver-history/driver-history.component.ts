import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RideApiService, DriverRideHistoryResponse } from '../../../core/services/ride-api.service';

@Component({
  selector: 'app-driver-history',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './driver-history.component.html',
  styleUrls: ['./driver-history.component.css']
})
export class DriverHistoryComponent implements OnInit {
  private rideApiService = inject(RideApiService);

  rides: any[] = [];
  dateFrom: string = '2024-01-01';
  dateTo: string = new Date().toISOString().split('T')[0];

  ngOnInit() {
    this.loadRideHistory();
  }

  loadRideHistory() {
    this.rideApiService.getDriverRideHistory(this.dateFrom, this.dateTo).subscribe({
      next: (response) => {
        this.rides = response.content.map(ride => this.mapRideToView(ride));
      },
      error: (err) => {
        console.error('Failed to load ride history', err);
      }
    });
  }

  private mapRideToView(ride: DriverRideHistoryResponse) {
    const status = [];
    if (ride.cancelled) {
      status.push(ride.cancelledBy === 'DRIVER' ? 'cancelled-driver' : 'cancelled-passenger');
    } else if (ride.status === 'COMPLETED') {
      status.push('completed');
    }
    if (ride.panicActivated) {
      status.push('panic');
    }

    const startTime = ride.startTime ? new Date(ride.startTime) : null;
    const endTime = ride.endTime ? new Date(ride.endTime) : null;
    const duration = startTime && endTime
      ? Math.round((endTime.getTime() - startTime.getTime()) / 60000)
      : null;

    return {
      date: startTime ? startTime.toLocaleDateString('en-GB', { day: 'numeric', month: 'long', year: 'numeric' }) : 'N/A',
      time: ride.cancelled
        ? 'Canceled'
        : (startTime && endTime
            ? `${startTime.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })} - ${endTime.toLocaleTimeString('en-GB', { hour: '2-digit', minute: '2-digit' })} (${duration} min)`
            : 'N/A'),
      status,
      locations: [
        { type: 'start', label: 'Start', address: ride.pickupLocation || 'N/A' },
        { type: 'end', label: 'Finish', address: ride.dropoffLocation || 'N/A' }
      ],
      price: ride.price ? `${ride.price.toFixed(2)} RSD` : null,
      cancelReason: ride.cancelledBy ? 'Cancelled' : null,
      passengers: ride.passengers.map(p => ({
        initials: `${p.firstName[0]}${p.lastName[0]}`,
        name: `${p.firstName} ${p.lastName}`
      }))
    };
  }

  onFilter() {
    this.loadRideHistory();
  }

  onReset() {
    this.dateFrom = '2024-01-01';
    this.dateTo = new Date().toISOString().split('T')[0];
    this.loadRideHistory();
  }
}
