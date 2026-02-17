import { Component, OnInit, inject, computed, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { RideApiService, RideStatsResponse } from '../../../core/services/ride-api.service';

@Component({
  selector: 'app-passenger-stats',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  templateUrl: './passenger-stats.component.html',
  styleUrl: './passenger-stats.component.css'
})
export class PassengerStatsComponent implements OnInit {
  private rideApiService = inject(RideApiService);
  private cdr = inject(ChangeDetectorRef);

  dateFrom = '2024-01-01';
  dateTo = new Date().toISOString().split('T')[0];

  loading = false;
  error?: string;
  stats: RideStatsResponse | null = null;

  ridesChartData = computed<ChartConfiguration<'bar'>['data']>(() => {
    const s = this.stats;
    if (!s?.dailyData?.length) return { labels: [], datasets: [] };
    return {
      labels: s.dailyData.map((d) => this.formatChartLabel(d.date)),
      datasets: [
        {
          label: 'Rides',
          data: s.dailyData.map((d) => d.rideCount),
          backgroundColor: 'rgba(34, 197, 94, 0.6)',
          borderColor: 'rgba(34, 197, 94, 1)',
          borderWidth: 1
        }
      ]
    };
  });

  distanceChartData = computed<ChartConfiguration<'bar'>['data']>(() => {
    const s = this.stats;
    if (!s?.dailyData?.length) return { labels: [], datasets: [] };
    return {
      labels: s.dailyData.map((d) => this.formatChartLabel(d.date)),
      datasets: [
        {
          label: 'Distance (km)',
          data: s.dailyData.map((d) => d.distanceKm),
          backgroundColor: 'rgba(59, 130, 246, 0.6)',
          borderColor: 'rgba(59, 130, 246, 1)',
          borderWidth: 1
        }
      ]
    };
  });

  amountChartData = computed<ChartConfiguration<'bar'>['data']>(() => {
    const s = this.stats;
    if (!s?.dailyData?.length) return { labels: [], datasets: [] };
    return {
      labels: s.dailyData.map((d) => this.formatChartLabel(d.date)),
      datasets: [
        {
          label: 'Spent (RSD)',
          data: s.dailyData.map((d) => Number(d.amount)),
          backgroundColor: 'rgba(168, 85, 247, 0.6)',
          borderColor: 'rgba(168, 85, 247, 1)',
          borderWidth: 1
        }
      ]
    };
  });

  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    },
    scales: {
      x: {
        ticks: { color: '#a1a1a1', maxRotation: 45 }
      },
      y: {
        beginAtZero: true,
        ticks: { color: '#a1a1a1' }
      }
    }
  };

  ngOnInit() {
    this.loadStats();
  }

  loadStats() {
    this.loading = true;
    this.error = undefined;
    this.rideApiService.getPassengerRideStats(this.dateFrom, this.dateTo).subscribe({
      next: (response) => {
        this.stats = response;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || err?.message || 'Failed to load statistics';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onFilter() {
    this.loadStats();
  }

  onReset() {
    this.dateFrom = '2024-01-01';
    this.dateTo = new Date().toISOString().split('T')[0];
    this.loadStats();
  }

  private formatChartLabel(dateStr: string): string {
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short' });
  }
}
