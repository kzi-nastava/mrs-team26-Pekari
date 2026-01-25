import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-activate',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './activate.component.html',
  styleUrl: './activate.component.css'
})
export class ActivateComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private authService = inject(AuthService);

  isLoading = signal(true);
  error = signal<string | null>(null);

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');

    if (!token) {
      this.error.set('Invalid activation link. Token is missing.');
      this.isLoading.set(false);
      return;
    }

    this.authService.activate(token).subscribe({
      next: () => {
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Activation failed', err);
        this.error.set(err.error?.message || 'Activation failed. The link may be expired or invalid.');
        this.isLoading.set(false);
      }
    });
  }
}
