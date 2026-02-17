import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { AdminService, UserListItem } from '../../../core/services/admin.service';

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit {
  private adminService = inject(AdminService);

  drivers = signal<UserListItem[]>([]);
  passengers = signal<UserListItem[]>([]);
  loading = signal(true);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  showBlockModal = signal(false);
  blockTarget = signal<UserListItem | null>(null);
  blockNote = signal('');
  blockSubmitting = signal(false);

  ngOnInit(): void {
    this.loadAll();
  }

  loadAll(): void {
    this.loading.set(true);
    this.errorMessage.set(null);
    forkJoin({
      drivers: this.adminService.getDrivers(),
      passengers: this.adminService.getPassengers()
    }).subscribe({
      next: ({ drivers, passengers }) => {
        this.drivers.set(drivers);
        this.passengers.set(passengers);
        this.loading.set(false);
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || err?.message || 'Failed to load users');
        this.loading.set(false);
      }
    });
  }

  openBlockModal(user: UserListItem): void {
    this.blockTarget.set(user);
    this.blockNote.set(user.blockedNote || '');
    this.showBlockModal.set(true);
  }

  closeBlockModal(): void {
    this.showBlockModal.set(false);
    this.blockTarget.set(null);
    this.blockNote.set('');
  }

  submitBlock(): void {
    const target = this.blockTarget();
    if (!target) return;
    this.blockSubmitting.set(true);
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.adminService.setUserBlock(target.id, true, this.blockNote().trim() || null).subscribe({
      next: (res) => {
        this.successMessage.set(res.message || 'User blocked.');
        this.closeBlockModal();
        this.blockSubmitting.set(false);
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || err?.message || 'Failed to block user');
        this.blockSubmitting.set(false);
      }
    });
  }

  unblock(user: UserListItem): void {
    this.successMessage.set(null);
    this.errorMessage.set(null);
    this.adminService.setUserBlock(user.id, false).subscribe({
      next: (res) => {
        this.successMessage.set(res.message || 'User unblocked.');
        this.loadAll();
      },
      error: (err) => {
        this.errorMessage.set(err?.error?.message || err?.message || 'Failed to unblock user');
      }
    });
  }

  fullName(user: UserListItem): string {
    return [user.firstName, user.lastName].filter(Boolean).join(' ') || user.email;
  }

  notePreview(note: string | null, maxLen: number = 40): string {
    if (!note || !note.trim()) return '—';
    return note.length <= maxLen ? note : note.slice(0, maxLen) + '…';
  }
}
