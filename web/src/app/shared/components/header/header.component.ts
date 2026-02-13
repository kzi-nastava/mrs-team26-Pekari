import { Component, input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';

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

  handleLinkClick(link: NavLink, event: Event) {
    if (link.onClick) {
      event.preventDefault();
      link.onClick();
    }
  }
}
