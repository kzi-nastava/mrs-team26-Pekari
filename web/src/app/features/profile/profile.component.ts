import { Component, inject, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl } from '@angular/forms';
import { ProfileService } from '../../core/services/profile.service';
import { AuthService } from '../../core/services/auth.service';
import { ProfileData, DriverInfo, ProfileUpdateRequest } from '../../core/models/profile.model';

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css'
})
export class ProfileComponent {
  private profileService = inject(ProfileService);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  profile = signal<ProfileData | null>(null);
  driverInfo = signal<DriverInfo | null>(null);
  profilePicture = signal<string>('');
  isLoading = signal(false);
  isEditing = signal(false);
  showPasswordModal = signal(false);
  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);
  selectedFile = signal<File | null>(null);

  profileForm = this.fb.group({
    name: ['', [Validators.required]],
    email: [{ value: '', disabled: true }],
    phoneNumber: ['', [Validators.required]],
    address: ['', [Validators.required]]
  });

  passwordForm = this.fb.group({
    currentPassword: ['', [Validators.required, Validators.minLength(6)]],
    newPassword: ['', [Validators.required, Validators.minLength(6)]],
    confirmPassword: ['', [Validators.required]]
  }, { validators: this.passwordMatchValidator });

  vehicleForm = this.fb.group({
    make: [{ value: '', disabled: true }],
    model: [{ value: '', disabled: true }],
    year: [{ value: '', disabled: true }],
    licensePlate: [{ value: '', disabled: true }],
    vin: [{ value: '', disabled: true }]
  });

  get currentUser() {
    return this.authService.currentUser();
  }

  get isDriver() {
    return this.currentUser?.role === 'driver';
  }

  constructor() {
    this.loadProfile();

    effect(() => {
      if (this.isDriver) {
        this.loadDriverInfo();
      }
    });

    this.profilePicture.set(this.profileService.getDefaultProfilePicture());
  }

  private loadProfile(): void {
    this.isLoading.set(true);
    this.profileService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        const fullName = `${profile.firstName} ${profile.lastName}`.trim();
        this.profileForm.patchValue({
          name: fullName,
          email: profile.email,
          phoneNumber: profile.phoneNumber,
          address: profile.address
        });
        if (profile.profilePicture) {
          this.profilePicture.set(profile.profilePicture);
        }
        this.isLoading.set(false);
      },
      error: (err) => {
        this.errorMessage.set('Failed to load profile');
        this.isLoading.set(false);
      }
    });
  }

  private loadDriverInfo(): void {
    this.profileService.getDriverInfo().subscribe({
      next: (info) => {
        this.driverInfo.set(info);
        if (info.vehicle) {
          this.vehicleForm.patchValue({
            make: info.vehicle.make,
            model: info.vehicle.model,
            year: info.vehicle.year.toString(),
            licensePlate: info.vehicle.licensePlate,
            vin: info.vehicle.vin
          });
        }
      },
      error: (err) => {
        console.error('Failed to load driver info', err);
      }
    });
  }

  /**
   * Handle profile picture upload
   */
  onProfilePictureChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.selectedFile.set(file);

      // Preview image
      const reader = new FileReader();
      reader.onload = (e) => {
        this.profilePicture.set(e.target?.result as string);
      };
      reader.readAsDataURL(file);
    }
  }

  /**
   * Toggle edit mode
   */
  toggleEditMode(): void {
    this.isEditing.set(!this.isEditing());
    if (!this.isEditing()) {
      this.loadProfile(); // Reset form if canceling
    }
  }

  /**
   * Submit profile update
   */
  onSubmitProfile(): void {
    if (this.profileForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);

      const fullName = this.profileForm.get('name')?.value || '';
      const nameParts = fullName.trim().split(' ');
      const firstName = nameParts[0] || '';
      const lastName = nameParts.slice(1).join(' ') || '';

      const updateData: ProfileUpdateRequest = {
        firstName: firstName,
        lastName: lastName,
        phoneNumber: this.profileForm.get('phoneNumber')?.value || '',
        address: this.profileForm.get('address')?.value || '',
        profilePicture: this.profilePicture()
      };

      if (this.isDriver && this.driverInfo()) {
        updateData.vehicle = this.driverInfo()!.vehicle;
      }

      this.profileService.updateProfile(updateData).subscribe({
        next: (response) => {
          const message = this.isDriver
            ? 'Profile update request sent for admin approval'
            : 'Profile updated successfully';
          this.showSuccessMessage(message);
          this.isEditing.set(false);
          this.loadProfile();
          this.isLoading.set(false);
        },
        error: (err) => {
          this.errorMessage.set('Failed to update profile. Please try again.');
          this.isLoading.set(false);
        }
      });
    }
  }

  /**
   * Open password change modal
   */
  openPasswordModal(): void {
    this.showPasswordModal.set(true);
    this.passwordForm.reset();
  }

  /**
   * Close password change modal
   */
  closePasswordModal(): void {
    this.showPasswordModal.set(false);
    this.passwordForm.reset();
  }

  /**
   * Submit password change
   */
  onSubmitPasswordChange(): void {
    if (this.passwordForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);

      const passwordData = {
        currentPassword: this.passwordForm.get('currentPassword')?.value || '',
        newPassword: this.passwordForm.get('newPassword')?.value || '',
        confirmPassword: this.passwordForm.get('confirmPassword')?.value || ''
      };

      this.profileService.changePassword(passwordData).subscribe({
        next: (response) => {
          this.successMessage.set('Password changed successfully');
          this.closePasswordModal();
          this.isLoading.set(false);
        },
        error: (err) => {
          this.errorMessage.set('Failed to change password. Please check your current password.');
          this.isLoading.set(false);
        }
      });
    }
  }

  /**
   * Show success message and auto-dismiss after 2 seconds
   */
  private showSuccessMessage(message: string): void {
    this.successMessage.set(message);
    setTimeout(() => {
      this.successMessage.set(null);
    }, 2000);
  }

  /**
   * Validator for password match
   */
  private passwordMatchValidator(control: AbstractControl) {
    const password = control.get('newPassword');
    const confirmPassword = control.get('confirmPassword');

    return password && confirmPassword && password.value !== confirmPassword.value
      ? { passwordMismatch: true }
      : null;
  }

  /**
   * Format hours to readable string (e.g., 5.33 -> 5h 20m)
   */
  formatHours(hours: number): string {
    if (!hours) return '0h 0m';
    const h = Math.floor(hours);
    const m = Math.round((hours - h) * 60);
    return `${h}h ${m}m`;
  }
}
