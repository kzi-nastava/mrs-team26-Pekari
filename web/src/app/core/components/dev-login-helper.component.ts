import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';
import { Router } from '@angular/router';

/**
 * DEVELOPMENT ONLY: Quick login helper for testing
 * This component provides quick buttons to test different user roles
 * Remove this in production
 */
@Component({
  selector: 'app-dev-login-helper',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dev-helper">
      <button class="dev-btn passenger-btn" (click)="quickLogin('passenger')">
        🧑 Login as Passenger
      </button>
      <button class="dev-btn driver-btn" (click)="quickLogin('driver')">
        🚕 Login as Driver
      </button>
      <button class="dev-btn admin-btn" (click)="quickLogin('admin')">
        👨‍💼 Login as Admin
      </button>
      <button class="dev-btn logout-btn" (click)="logout()">
        🚪 Logout
      </button>
    </div>
  `,
  styles: [`
    .dev-helper {
      position: fixed;
      bottom: 20px;
      right: 20px;
      display: flex;
      flex-direction: column;
      gap: 10px;
      z-index: 9999;
      background: rgba(0, 0, 0, 0.8);
      padding: 15px;
      border-radius: 8px;
      border: 1px solid rgba(76, 175, 80, 0.5);
    }

    .dev-btn {
      padding: 8px 12px;
      border: none;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      white-space: nowrap;
      text-transform: uppercase;
    }

    .passenger-btn {
      background: rgba(33, 150, 243, 0.2);
      color: #2196f3;
      border: 1px solid rgba(33, 150, 243, 0.5);
    }

    .passenger-btn:hover {
      background: rgba(33, 150, 243, 0.3);
      border-color: #2196f3;
    }

    .driver-btn {
      background: rgba(255, 152, 0, 0.2);
      color: #ff9800;
      border: 1px solid rgba(255, 152, 0, 0.5);
    }

    .driver-btn:hover {
      background: rgba(255, 152, 0, 0.3);
      border-color: #ff9800;
    }

    .admin-btn {
      background: rgba(244, 67, 54, 0.2);
      color: #f44336;
      border: 1px solid rgba(244, 67, 54, 0.5);
    }

    .admin-btn:hover {
      background: rgba(244, 67, 54, 0.3);
      border-color: #f44336;
    }

    .logout-btn {
      background: rgba(76, 175, 80, 0.2);
      color: #4caf50;
      border: 1px solid rgba(76, 175, 80, 0.5);
    }

    .logout-btn:hover {
      background: rgba(76, 175, 80, 0.3);
      border-color: #4caf50;
    }

    @media (max-width: 768px) {
      .dev-helper {
        bottom: 10px;
        right: 10px;
        flex-direction: row;
        flex-wrap: wrap;
        width: auto;
        max-width: 300px;
      }

      .dev-btn {
        flex: 1;
        min-width: 70px;
        padding: 6px 8px;
        font-size: 11px;
      }
    }
  `]
})
export class DevLoginHelperComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  quickLogin(role: 'passenger' | 'driver' | 'admin'): void {
    const testCredentials: Record<string, { email: string; password: string }> = {
      passenger: {
        email: 'passenger1@test.com',
        password: 'Pass1234'
      },
      driver: {
        email: 'driver_std@test.com',
        password: 'Driver1234'
      },
      admin: {
        email: 'admin@test.com',
        password: 'Admin1234'
      }
    };

    const credentials = testCredentials[role];

    this.authService.login(credentials).subscribe({
      next: (user) => {
        // Navigate based on role
        if (user.role === 'driver') {
          this.router.navigate(['/driver-history']);
        } else if (user.role === 'passenger') {
          this.router.navigate(['/passenger-home']);
        } else if (user.role === 'admin') {
          this.router.navigate(['/admin/add-driver']);
        } else {
          this.router.navigate(['/profile']);
        }
      },
      error: (err) => {
        console.error('Dev login failed:', err);
        alert(`Login failed: ${err.message}`);
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/'])
    });
  }
}
