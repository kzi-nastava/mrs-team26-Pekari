import { Component, input, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { NotificationService } from '../../../core/services/notification.service';

export interface NavLink {
  label: string;
  path?: string;
  onClick?: () => void;
  /** When true, style the link as danger (e.g. red for blocked users) */
  danger?: boolean;
}

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent {
  title = input<string>('My App');
  navLinksLeft = input<NavLink[]>([]);
  navLinksRight = input<NavLink[]>([]);

  notificationService = inject(NotificationService);
  private router = inject(Router);

  showNotifications = signal(false);

  toggleNotifications() {
    this.showNotifications.update(v => !v);
    if (this.showNotifications()) {
      this.notificationService.markAllAsRead();
    }
  }

  goToRide(rideId: number) {
    this.showNotifications.set(false);
    this.router.navigate(['/rides', rideId, 'track']);
  }

  handleLinkClick(link: NavLink, event: Event) {
    if (link.onClick) {
      event.preventDefault();
      link.onClick();
    }
  }
}
