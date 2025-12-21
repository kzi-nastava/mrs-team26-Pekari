import { Component, inject, computed } from '@angular/core';
import { RouterOutlet } from '@angular/router';
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

  navLinksLeft = computed(() => {
    return [] as NavLink[];
  });

  navLinksRight = computed(() => {
    const user = this.authService.currentUser();
    const links: NavLink[] = [];

    if (!user) {
      links.push(
        { label: 'Login', path: '/login' },
        { label: 'Sign Up', path: '/signup' }
      );
    } else {
      if (user.role === 'driver') {
        links.push({ label: 'History', path: '/history' });
      }
      links.push(
        { label: 'Profile', path: '/profile' },
        { label: 'Logout', onClick: () => this.authService.logout() }
      );
    }

    return links;
  });
}
