import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private authService = inject(AuthService);

  forgotPasswordForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]]
  });

  successMessage = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  onSubmit(): void {
    if (this.forgotPasswordForm.valid) {
      const email = this.forgotPasswordForm.value.email!;
      this.successMessage.set(null);
      this.errorMessage.set(null);

      this.authService.forgotPassword(email).subscribe({
        next: () => {
          this.successMessage.set('Instructions for changing password sent to email.');
          this.forgotPasswordForm.reset();
        },
        error: (err: any) => {
          this.errorMessage.set('Something went wrong. Please try again later.');
          console.error('Forgot password failed', err);
        }
      });
    }
  }
}
