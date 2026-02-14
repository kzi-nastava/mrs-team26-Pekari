import { Routes } from '@angular/router';
import { LandingComponent } from './features/landing/landing.component';
import { LoginComponent } from './features/auth/login/login.component';
import { RegisterComponent } from './features/auth/register/register.component';
import { ActivateComponent } from './features/auth/activate/activate.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { DriverHomeComponent } from './features/driver/driver-home/driver-home.component';
import { DriverHistoryComponent } from './features/driver/driver-history/driver-history.component';
import { PassengerHomeComponent } from './features/passenger/passenger-home/passenger-home.component';
import { PassengerHistoryComponent } from './features/passenger/passenger-history/passenger-history.component';
import { ProfileComponent } from './features/profile/profile.component';
import { AddDriverComponent } from './features/admin/add-driver/add-driver.component';
import { PanicPanel } from './features/admin/panic-panel/panic-panel';
import { UserManagementComponent } from './features/admin/user-management/user-management.component';
import { AdminRidesComponent } from './features/admin/admin-rides/admin-rides.component';
import { RideTrackingComponent } from './features/passenger/ride-tracking/ride-tracking.component';
import { authGuard, roleGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', component: LandingComponent, canActivate: [authGuard] },
  { path: 'login', component: LoginComponent, canActivate: [authGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [authGuard] },
  { path: 'activate', component: ActivateComponent },
  { path: 'forgot-password', component: ForgotPasswordComponent },
  { path: 'profile', component: ProfileComponent, canActivate: [roleGuard(['admin', 'passenger', 'driver'])] },
  {
    path: 'driver-home',
    component: DriverHomeComponent,
    canActivate: [roleGuard(['driver'])]
  },
  {
    path: 'driver-history',
    component: DriverHistoryComponent,
    canActivate: [roleGuard(['driver'])]
  },
  {
    path: 'passenger-home',
    component: PassengerHomeComponent,
    canActivate: [roleGuard(['passenger'])]
  },
  {
    path: 'passenger-history',
    component: PassengerHistoryComponent,
    canActivate: [roleGuard(['passenger'])]
  },
  {
    path: 'rides/:id/track',
    component: RideTrackingComponent,
    canActivate: [roleGuard(['passenger'])]
  },
  {
    path: 'admin/add-driver',
    component: AddDriverComponent,
    canActivate: [roleGuard(['admin'])]
  },
  {
    path: 'admin/panic-panel',
    component: PanicPanel,
    canActivate: [roleGuard(['admin'])]
  },
  {
    path: 'admin/user-management',
    component: UserManagementComponent,
    canActivate: [roleGuard(['admin'])]
  },
  {
    path: 'admin/rides',
    component: AdminRidesComponent,
    canActivate: [roleGuard(['admin'])]
  }
];
