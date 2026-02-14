import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminService, Pricing } from '../../../core/services/admin.service';

@Component({
  selector: 'app-pricing-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pricing-management.component.html',
  styleUrls: ['./pricing-management.component.css']
})
export class PricingManagementComponent implements OnInit {
  private adminService = inject(AdminService);

  pricingList = signal<Pricing[]>([]);
  loading = signal<boolean>(false);
  error = signal<string | null>(null);
  success = signal<string | null>(null);

  editingPricing = signal<Pricing | null>(null);

  ngOnInit(): void {
    this.loadPricing();
  }

  loadPricing(): void {
    this.loading.set(true);
    this.adminService.getPricing().subscribe({
      next: (data) => {
        this.pricingList.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set('Failed to load pricing data');
        this.loading.set(false);
      }
    });
  }

  startEdit(pricing: Pricing): void {
    this.editingPricing.set({ ...pricing });
    this.success.set(null);
    this.error.set(null);
  }

  cancelEdit(): void {
    this.editingPricing.set(null);
  }

  savePricing(): void {
    const pricing = this.editingPricing();
    if (!pricing) return;

    this.loading.set(true);
    this.adminService.updatePricing(pricing).subscribe({
      next: (updated) => {
        this.pricingList.update(list => list.map(p => p.vehicleType === updated.vehicleType ? updated : p));
        this.editingPricing.set(null);
        this.success.set('Pricing updated successfully');
        this.loading.set(false);
        setTimeout(() => this.success.set(null), 3000);
      },
      error: (err) => {
        this.error.set('Failed to update pricing');
        this.loading.set(false);
      }
    });
  }
}
