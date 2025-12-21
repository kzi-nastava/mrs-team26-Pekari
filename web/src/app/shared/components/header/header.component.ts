import { Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  title = input<string>('My App');
  authService = inject(AuthService);
  currentUser = this.authService.currentUser;

  logout() {
    this.authService.logout();
  }
  navLinksLeft = [
    { label: 'Pregled', path: '/pregled' },
    { label: 'Istorija', path: '/istorija' },
    { label: 'Profil', path: '/profil' }
  ];
  navLinksRight = [
    { label: 'Odjavi se', path: '/logout' }
  ];
}
