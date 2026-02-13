import { Component, inject, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { EnvironmentService } from './core/services/environment.service';
import { HeaderComponent, type NavLink } from './shared/components/header/header.component';
import { DevLoginHelperComponent } from './core/components/dev-login-helper.component';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, HeaderComponent, DevLoginHelperComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private authService = inject(AuthService);
  private router = inject(Router);
  private environmentService = inject(EnvironmentService);
  title = 'BlackCar';

  navLinksLeft = computed(() => {
    return [] as NavLink[];
  });

  navLinksRight = computed(() => {
    const user = this.authService.currentUser();
    const links: NavLink[] = [];

    if (!user) {
      links.push(
        { label: 'Login', path: '/login' },
        { label: 'Register', path: '/register' }
      );
    } else if (user.role === 'driver') {
      links.push(
        { label: 'Home', path: '/driver-home' },
        { label: 'Profile', path: '/profile', danger: user.blocked },
        { label: 'History', path: '/driver-history' },
        { label: 'Logout', onClick: () => this.handleLogout() }
      );
    } else if (user.role === 'passenger') {
      links.push(
        { label: 'Home', path: '/passenger-home' },
        { label: 'History', path: '/passenger-history' },
        { label: 'Profile', path: '/profile', danger: user.blocked },
        { label: 'Logout', onClick: () => this.handleLogout() }
      );
    } else if (user.role === 'admin') {
      links.push(
        { label: 'User management', path: '/admin/user-management' },
        { label: 'Add Driver', path: '/admin/add-driver' },
        { label: '🚨 Panic Panel', path: '/admin/panic-panel' },
        { label: 'Logout', onClick: () => this.handleLogout() }
      );
    }

    return links;
  });

  handleLogout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/']),
      error: () => this.router.navigate(['/']) // Navigate even if logout fails
    });
  }

  // Show development helper based on environment configuration
  isDevelopment = this.environmentService.areDevToolsEnabled();
}
