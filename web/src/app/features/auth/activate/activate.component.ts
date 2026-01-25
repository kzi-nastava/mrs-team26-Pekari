import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-activate',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './activate.component.html',
  styleUrl: './activate.component.css'
})
export class ActivateComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);
  private fb = inject(FormBuilder);

  isLoading = signal(true);
  error = signal<string | null>(null);
  success = signal<string | null>(null);
  requiresPassword = signal(false);
  tokenValue: string | null = null;

  passwordForm = this.fb.group({
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    const mode = this.route.snapshot.queryParamMap.get('mode');

    if (!token) {
      this.error.set('Invalid activation link. Token is missing.');
      this.isLoading.set(false);
      return;
    }

    this.tokenValue = token;

    if (mode === 'driver') {
      this.requiresPassword.set(true);
      this.isLoading.set(false);
      return;
    }

    this.authService.getActivationInfo(token).subscribe({
      next: (info) => {
        if (info.requiresPassword) {
          this.requiresPassword.set(true);
          this.isLoading.set(false);
          return;
        }

        this.authService.activate(token).subscribe({
          next: () => {
            this.isLoading.set(false);
          },
          error: (err) => {
            console.error('Activation failed', err);
            this.error.set(err.message || 'Activation failed. The link may be expired or invalid.');
            this.isLoading.set(false);
          }
        });
      },
      error: (err) => {
        this.error.set(err.message || 'Activation failed. The link may be expired or invalid.');
        this.isLoading.set(false);
      }
    });
  }

  onSubmitPassword() {
    if (!this.tokenValue) {
      this.error.set('Invalid activation link. Token is missing.');
      return;
    }

    if (this.passwordForm.invalid) {
      this.passwordForm.markAllAsTouched();
      return;
    }

    const newPassword = this.passwordForm.get('newPassword')?.value || '';
    const confirmPassword = this.passwordForm.get('confirmPassword')?.value || '';

    if (newPassword !== confirmPassword) {
      this.error.set('Passwords do not match.');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    this.authService.setNewPassword(this.tokenValue, newPassword).subscribe({
      next: (resp) => {
        this.success.set(resp.message || 'Password set successfully. You can now log in.');
        this.isLoading.set(false);
      },
      error: (err) => {
        this.error.set(err.message || 'Password setup failed. The link may be expired or invalid.');
        this.isLoading.set(false);
      }
    });
  }
}
