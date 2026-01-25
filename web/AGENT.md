# BlackCar Web Application - Agent Context

## Overview
Angular 21 single-page application for a ride-hailing service (BlackCar). Built with standalone components, signals, and functional guards.

## Technology Stack
- **Framework**: Angular 21
- **Language**: TypeScript 5.9
- **State Management**: Angular Signals
- **HTTP**: RxJS 7.8 with operators
- **Styling**: CSS (no UI framework)
- **Package Manager**: npm 11.6.1

## Project Architecture

### Directory Structure
```
web/src/app/
├── core/                    # Core functionality
│   ├── guards/              # Route protection
│   │   └── auth.guard.ts    # authGuard, roleGuard
│   ├── interceptors/
│   │   └── auth.interceptor.ts  # Adds JWT to HTTP requests
│   ├── models/              # TypeScript interfaces
│   │   ├── user.model.ts    # User interface with roles
│   │   └── profile.model.ts # ProfileData, DriverInfo, etc.
│   ├── services/            # API communication
│   │   ├── auth.service.ts  # Authentication & registration
│   │   ├── profile.service.ts   # User profiles
│   │   ├── ride-api.service.ts  # Ride estimates & orders
│   │   └── environment.service.ts  # Config management
│   └── components/
│       └── dev-login-helper.component.ts  # Dev mode login shortcut
├── features/                # Feature modules by user role
│   ├── auth/                # Authentication flows
│   │   ├── login/
│   │   ├── register/
│   │   ├── activate/        # Email activation
│   │   └── forgot-password/
│   ├── admin/
│   │   └── add-driver/      # Admin registers new drivers
│   ├── driver/
│   │   └── driver-history/  # Driver's ride history & dashboard
│   ├── passenger/
│   │   └── passenger-home/  # Ride ordering interface
│   ├── profile/             # User profile management
│   └── landing/             # Public landing page
└── shared/
    └── components/
        └── header/          # Navigation header

environments/
├── environment.ts           # Development config
├── environment.staging.ts   # Staging config
└── environment.production.ts # Production config
```

## User Roles & Permissions

### Three User Types
1. **Admin** (`admin`)
   - Register new drivers
   - Approve profile change requests (drivers require admin approval)
   - Access: `/admin/add-driver`, `/profile`

2. **Driver** (`driver`)
   - View and accept ride requests
   - View ride history
   - Profile changes require admin approval
   - Access: `/driver-history`, `/profile`

3. **Passenger** (`passenger`)
   - Order rides (immediate or scheduled)
   - Share rides with other passengers
   - View ride history
   - Access: `/passenger-home`, `/profile`

## Routing & Guards

### Routes (`app.routes.ts`)
```typescript
/ → LandingComponent [authGuard - redirects if logged in]
/login → LoginComponent [authGuard]
/register → RegisterComponent [authGuard]
/activate → ActivateComponent [public - email activation]
/forgot-password → ForgotPasswordComponent [public]
/profile → ProfileComponent [roleGuard: all roles]
/driver-history → DriverHistoryComponent [roleGuard: driver only]
/passenger-home → PassengerHomeComponent [roleGuard: passenger only]
/admin/add-driver → AddDriverComponent [roleGuard: admin only]
```

### Guard Behavior
- **authGuard**: Redirects logged-in users away from public pages (login/register) to their role-specific home
- **roleGuard**: Checks role, redirects unauthorized users to their home page or login

## Core Services

### AuthService (`core/services/auth.service.ts`)
**Purpose**: Authentication, user registration, session management

**Key Features**:
- Signal-based current user state: `currentUserSignal`
- Stores JWT token, email, role in localStorage
- Auto-loads session on app init via `checkSession()`

**Methods**:
- `login(email, password)` → Returns Observable with token & role
- `register(userData)` → Register passenger, sends activation email
- `registerDriver(driverData: RegisterDriverData)` → Admin registers driver (backend sends activation email)
- `activateAccount(token)` → Email activation
- `logout()` → Clear session
- `normalizeRole(role)` → Converts backend role format to 'admin'|'passenger'|'driver'

**RegisterDriverData Interface**:
```typescript
{
  firstName, lastName, email, address, phoneNumber,
  vehicleModel, vehicleType, licensePlate, numberOfSeats,
  babyFriendly, petFriendly
}
```

### ProfileService (`core/services/profile.service.ts`)
**Purpose**: Fetch and update user profiles (different endpoints for driver/passenger)

**Key Methods**:
- `getProfile()` → Auto-detects role, calls appropriate endpoint
  - `/profile/driver` for drivers
  - `/profile/passenger` for passengers/admins
- `updateProfile(data)` → Update profile (drivers need admin approval)
- `getDriverInfo()` → Driver-specific data (currently mock, TODO for backend)
- `changePassword(passwordData)` → Change password (TODO for backend)
- `uploadProfilePicture(file)` → Upload picture (TODO for backend)

**Backend Response Mapping**:
- Maps backend snake_case/camelCase to frontend ProfileData model
- Converts ISO date strings to Date objects

### RideApiService (`core/services/ride-api.service.ts`)
**Purpose**: Ride estimation and ordering

**Methods**:
- `estimateRide(request: EstimateRideRequest)` → Get price/time estimate
  - Requires: pickup, dropoff (LocationPoint), vehicleType, babyTransport, petTransport
  - Returns: estimatedPrice, estimatedDurationMinutes, distanceKm

- `orderRide(request: OrderRideRequest)` → Create ride order
  - Requires: pickup, dropoff, vehicleType, babyTransport, petTransport
  - Optional: stops (intermediate locations), passengerEmails (shared rides), scheduledAt (scheduled rides)
  - Returns: rideId, status, message, estimatedPrice, scheduledAt, assignedDriverEmail

**LocationPoint Interface**:
```typescript
{
  address: string;
  latitude: number;
  longitude: number;
}
```

### EnvironmentService (`core/services/environment.service.ts`)
**Purpose**: Centralized environment configuration

**Usage**: `this.env.getApiUrl()` returns API base URL based on build config

## Authentication Flow

### Session Management
1. **Login**: POST `/auth/login` → Receive JWT token
2. **Store**: Save to localStorage: `auth_token`, `auth_email`, `auth_role`
3. **Restore**: On app init, `AuthService.checkSession()` reads localStorage and restores user signal
4. **Intercept**: `authInterceptor` adds `Authorization: Bearer {token}` to all HTTP requests
5. **Logout**: Clear localStorage and reset signal

### Registration Flows
- **Passenger Registration**: Self-service at `/register` → Email activation required
- **Driver Registration**: Admin-only at `/admin/add-driver` → Backend sends activation email to driver

### Activation
- User clicks email link with token → `/activate?token=xxx`
- Frontend calls backend activation endpoint
- On success, redirects to login

## Ride Workflow (Passenger)

1. **Passenger navigates to `/passenger-home`**
2. **Enters ride details**: pickup, dropoff, vehicle type, special requirements (baby/pet)
3. **Estimate**: Calls `rideApiService.estimateRide()` → Shows price/duration
4. **Order Options**:
   - **Immediate ride**: `scheduledAt = null`
   - **Scheduled ride**: Set future `scheduledAt` timestamp
   - **Shared ride**: Add `passengerEmails` array (other passengers to share with)
   - **Multi-stop**: Add `stops` array (intermediate locations)
5. **Confirm**: Calls `rideApiService.orderRide()`
6. **Backend assigns driver** (if available) and returns ride details

## HTTP & Error Handling

### Interceptors
- **authInterceptor** (`core/interceptors/auth.interceptor.ts`):
  - Adds JWT from localStorage to all requests
  - Excludes `/auth/login` and `/auth/register` endpoints

### RxJS Patterns
- **Imports**: All operators from `rxjs/operators` (Angular 21 / RxJS 7)
  - `map`, `switchMap`, `catchError` from `'rxjs/operators'`
  - `Observable`, `of`, `throwError` from `'rxjs'`
- **Error Handling**: Use `catchError` with `throwError(() => new Error(...))`

## Common Development Commands

```bash
npm install -g @angular/cli  # Install Angular CLI
ng serve                     # Dev server (default config)
npm run dev                  # Dev server
npm run staging              # Staging config
npm run prod                 # Production config
npm run build:prod           # Production build
```

## Key Patterns & Conventions

### Standalone Components
- All components use `standalone: true`
- Import dependencies directly in component metadata

### Signals for State
- `signal()` for writable signals
- `.asReadonly()` for public read-only signals
- Use `()` to read signal value: `authService.currentUser()`

### Dependency Injection
- Modern `inject()` function instead of constructor injection
- Example: `private http = inject(HttpClient)`

### Guards
- Functional guards (CanActivateFn) instead of class-based
- Return `true` | `UrlTree` | `false`

## Backend Integration

### API Endpoints (examples from code)
```
POST /auth/login              # Login
POST /auth/register           # Register passenger
POST /auth/activate           # Activate account
POST /admin/drivers/register  # Admin registers driver
GET  /profile/driver          # Get driver profile
GET  /profile/passenger       # Get passenger profile
PUT  /profile/driver          # Update driver profile (needs approval)
PUT  /profile/passenger       # Update passenger profile
POST /rides/estimate          # Estimate ride
POST /rides/order             # Order ride
```

### Response Formats
- Backend returns camelCase JSON
- Frontend models use TypeScript interfaces
- Date fields as ISO strings, converted to Date objects

## TODO Items (From Code Comments)

1. **ProfileService**:
   - Implement actual `getDriverInfo()` endpoint (currently returns mock)
   - Implement `changePassword()` endpoint
   - Implement `uploadProfilePicture()` endpoint
   - Implement `getApprovalRequests()` for admin
   - Implement `approveProfileUpdate()` for admin
   - Implement `rejectProfileUpdate()` for admin

2. **Driver Profile Approvals**:
   - Admin interface to view pending profile change requests
   - Approve/reject workflow

## Important Notes for Agents

- **Role-based logic**: Always check `currentUser().role` before API calls
- **Guard redirects**: Users automatically redirected to appropriate home pages
- **Driver profile updates**: Require admin approval (different from passenger updates)
- **Scheduled rides**: Optional `scheduledAt` field (ISO string or null)
- **Shared rides**: Use `passengerEmails` array in OrderRideRequest
- **Environment**: API URLs configured per environment (dev/staging/prod)
- **No UI framework**: Custom CSS styling, no Bootstrap/Material
- **RxJS version**: 7.8 - import operators from `rxjs/operators`, not `rxjs`
