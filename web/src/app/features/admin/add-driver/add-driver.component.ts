import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService, RegisterDriverData } from '../../../core/services/auth.service';

@Component({
  selector: 'app-add-driver',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './add-driver.component.html',
  styleUrl: './add-driver.component.css'
})
export class AddDriverComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  isSubmitting = signal(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  driverForm = this.fb.nonNullable.group({
    // Driver Information
    firstName: ['', [Validators.required, Validators.minLength(2)]],
    lastName: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]],
    address: ['', [Validators.required]],
    phoneNumber: ['', [Validators.required, Validators.pattern(/^\+?[0-9]{10,15}$/)]],

    // Vehicle Specifications
    vehicleModel: ['', [Validators.required]],
    vehicleType: ['STANDARD', [Validators.required]],
    licensePlate: ['', [Validators.required]],
    numberOfSeats: [4, [Validators.required, Validators.min(1), Validators.max(8)]],
    babyFriendly: [false],
    petFriendly: [false]
  });

  vehicleTypes = [
    { value: 'STANDARD', label: 'Standard' },
    { value: 'COMFORT', label: 'Comfort' },
    { value: 'VAN', label: 'Van' },
    { value: 'LUXURY', label: 'Luxury' }
  ];

  onSubmit(): void {
    if (this.driverForm.valid && !this.isSubmitting()) {
      this.isSubmitting.set(true);
      this.errorMessage.set(null);
      this.successMessage.set(null);

      const formData: RegisterDriverData = this.driverForm.getRawValue();

      this.authService.registerDriver(formData).subscribe({
        next: () => {
          this.successMessage.set('Driver account created successfully. An activation link has been sent to the driver\'s email address.');
          this.driverForm.reset({
            vehicleType: 'STANDARD',
            numberOfSeats: 4,
            babyFriendly: false,
            petFriendly: false
          });
          this.isSubmitting.set(false);
        },
        error: (err) => {
          this.errorMessage.set(err.message || 'Failed to create driver account. Please try again.');
          this.isSubmitting.set(false);
        }
      });
    }
  }
}
