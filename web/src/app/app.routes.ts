import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { DriverHistoryComponent } from './features/driver/driver-history/driver-history.component';
import { PassengerHomeComponent } from './features/passenger/passenger-home/passenger-home.component';
import { ProfileComponent } from './features/profile/profile.component';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent, canActivate: [authGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [authGuard] },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [roleGuard(['admin', 'passenger', 'driver'])] },
  {
    path: 'driver-history',
    component: DriverHistoryComponent,
    canActivate: [roleGuard(['driver'])]
  },
  {
    path: 'passenger-home',
    component: PassengerHomeComponent,
    canActivate: [roleGuard(['passenger'])]
  }
];
