import { inject } from '@angular/core';
import { Router, CanActivateFn } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const user = authService.currentUser();

  if (!user) {
    return true;
  }

  // If user is logged in and tries to access root or auth pages, redirect to their home
  if (state.url === '/' || state.url === '/login' || state.url === '/register') {
    if (user.role === 'driver') {
      return router.createUrlTree(['/driver-home']);
    } else if (user.role === 'passenger') {
      return router.createUrlTree(['/passenger-home']);
    }
  }

  return true;
};

export const roleGuard = (allowedRoles: string[]): CanActivateFn => {
  return (route, state) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const user = authService.currentUser();

    if (!user) {
      return router.createUrlTree(['/login']);
    }

    if (allowedRoles.includes(user.role)) {
      return true;
    }

    // Redirect to respective home if role not allowed
    if (user.role === 'driver') {
      return router.createUrlTree(['/driver-home']);
    } else if (user.role === 'passenger') {
      return router.createUrlTree(['/passenger-home']);
    }

    return router.createUrlTree(['/']);
  };
};
