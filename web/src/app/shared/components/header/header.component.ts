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
  navLinksLeft = [
    { label: 'Pregled', path: '/pregled' },
    { label: 'Istorija', path: '/istorija' },
    { label: 'Profil', path: '/profil' }
  ];
  navLinksRight = [
    { label: 'Odjavi se', path: '/logout' }
  ];
}
