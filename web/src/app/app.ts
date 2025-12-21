import { Component, inject, computed } from '@angular/core';
import { RouterOutlet, Router } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { HeaderComponent, type NavLink } from './shared/components/header/header.component';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HeaderComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private authService = inject(AuthService);
  private router = inject(Router);
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
        { label: 'History', path: '/driver-history' },
        { label: 'Logout', onClick: () => this.handleLogout() }
      );
    } else if (user.role === 'passenger') {
      links.push(
        { label: 'Home', path: '/passenger-home' },
        { label: 'Logout', onClick: () => this.handleLogout() }
      );
    }

    return links;
  });

  handleLogout() {
    this.authService.logout();
    this.router.navigate(['/']);
  }
}
