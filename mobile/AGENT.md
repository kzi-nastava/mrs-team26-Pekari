# Mobile Development Guidelines - BlackCar Project

This document provides essential information for AI agents and developers working on the Android mobile module of the BlackCar project.

## Project Overview
BlackCar is a premium ride-hailing application.
- **Backend**: Spring Boot API (`/api/v1` prefix, `http://10.0.2.2:8080/api/v1`)
- **Mobile**: Native Android application (Java, minSdk 24, targetSdk 36)
- **Architecture**: MVVM + Repository Pattern
- **Navigation**: Android Navigation Component

## Technology Stack
- **Language**: Java 11
- **Build**: Gradle (Kotlin DSL)
- **UI**: ViewBinding, Material Design, ConstraintLayout
- **Architecture Components**: LiveData, ViewModel, Lifecycle
- **Networking**: Retrofit 2.9.0, OkHttp 4.11.0, GSON
- **Navigation**: AndroidX Navigation Component

## Architecture: MVVM + Repository Pattern

### 1. Presentation Layer (`presentation/`)
- **Views (Fragments/Activities)**: Use ViewBinding, observe LiveData, render UI
- **ViewModels**: Encapsulate UI logic, interact with Repositories, expose ViewState
- **ViewState**: Immutable state classes (Idle, Loading, Success, Error)
- **ViewModelFactory**: Factory for creating ViewModels with dependencies

### 2. Data Layer (`data/`)
- **Repositories**: Single source of truth, handle error mapping
- **API Services**: Retrofit interfaces (AuthApiService, ProfileApiService, RideApiService)
- **API Models**: Request/Response POJOs
- **SessionManager**: In-memory session storage (token, email, role, userId)
- **TokenManager**: Persistent storage via SharedPreferences

## Project Structure

```
mobile/
├── app/
│   ├── build.gradle.kts              # Build configuration, dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/blackcar/
│       │   ├── data/
│       │   │   ├── api/
│       │   │   │   ├── ApiClient.java           # Retrofit singleton
│       │   │   │   ├── model/                   # API request/response models
│       │   │   │   └── service/                 # API service interfaces
│       │   │   ├── auth/
│       │   │   │   └── TokenManager.java        # Persistent token storage
│       │   │   ├── repository/
│       │   │   │   ├── AuthRepository.java
│       │   │   │   ├── ProfileRepository.java
│       │   │   │   └── RideRepository.java
│       │   │   └── session/
│       │   │       └── SessionManager.java      # In-memory session
│       │   └── presentation/
│       │       ├── ViewModelFactory.java
│       │       ├── auth/
│       │       │   ├── views/                   # LoginFragment, RegisterFragment, ForgotPasswordFragment
│       │       │   ├── viewmodel/               # LoginViewModel, RegisterViewModel, ForgotPasswordViewModel
│       │       │   └── viewstate/               # ViewState classes
│       │       ├── home/
│       │       │   ├── MainActivity.java        # Main activity with bottom navigation
│       │       │   └── views/                   # HomeFragment
│       │       ├── profile/
│       │       │   ├── views/                   # ProfileFragment
│       │       │   ├── viewmodel/               # ProfileViewModel
│       │       │   ├── viewstate/               # ProfileViewState
│       │       │   ├── model/                   # UI models (ProfileUIModel, DriverInfoUIModel, etc.)
│       │       │   └── data/                    # Mock data stores
│       │       └── history/
│       │           ├── views/                   # DriverHistoryFragment, DriverHistoryAdapter
│       │           ├── viewmodel/               # DriverHistoryViewModel, MockRideDataHelper
│       │           └── viewstate/               # DriverHistoryViewState, RideUIModel
│       └── res/
│           ├── layout/                          # XML layouts
│           ├── navigation/
│           │   └── nav_graph.xml               # Navigation graph
│           ├── menu/
│           │   └── bottom_nav_menu.xml         # Bottom navigation menu
│           ├── drawable/                        # Icons and drawables
│           └── values/                          # Strings, colors, themes, dimens
├── build.gradle.kts                            # Project-level Gradle
├── gradle/                                     # Gradle wrapper & version catalog
└── settings.gradle.kts
```

## Implemented Features

### Authentication (`presentation/auth/`)
- **Login**: Email/password authentication, JWT token storage
- **Register**: User registration with email, password, name, phone
- **Forgot Password**: Password reset flow

### Profile (`presentation/profile/`)
- **View Profile**: Display driver/passenger profile information
- **Edit Profile**: Update name, phone, profile picture
- **Change Password**: Secure password change
- **Driver Info**: Vehicle information, approval requests
- **Logout**: Clear session and navigate to login

### Ride History (`presentation/history/`)
- **Driver History**: List of completed rides with pagination
- **Ride Details**: Pickup/dropoff locations, fare, duration, date

### Home (`presentation/home/`)
- **MainActivity**: Bottom navigation between Home, History, Profile
- **HomeFragment**: Landing page after login

## Navigation Flow
Start: `LoginFragment` → `HomeFragment` (after login)
- Login → Register
- Login → Forgot Password
- Home → Driver History
- Profile → Login (after logout)

## Key Components

### API Configuration
- **Base URL**: `http://10.0.2.2:8080/api/v1` (emulator points to host machine)
- **Authentication**: Cookie-based authentication
- **Logging**: OkHttp logging interceptor enabled

### Session Management
- **SessionManager**: In-memory session (token, email, role, userId)
- **TokenManager**: Persistent storage via SharedPreferences

### ViewState Pattern
All features use a sealed ViewState pattern:
```java
abstract class FeatureViewState {
    static class Idle extends FeatureViewState {}
    static class Loading extends FeatureViewState {}
    static class Success extends FeatureViewState { /* data */ }
    static class Error extends FeatureViewState { String message; }
}
```

## Guidelines for Development

1. **Consistency**: Follow MVVM pattern strictly. Keep fragments lean.
2. **Error Handling**: Map errors in Repository, not in ViewModel or Fragment.
3. **ViewBinding**: Always use ViewBinding, never findViewById.
4. **Navigation**: Use Navigation Component actions defined in nav_graph.xml.
5. **API Models**: Create separate request/response models, don't reuse UI models.
6. **Naming Conventions**:
   - Fragments: `FeatureFragment.java`
   - ViewModels: `FeatureViewModel.java`
   - ViewStates: `FeatureViewState.java`
   - Repositories: `FeatureRepository.java`
   - API Services: `FeatureApiService.java`

## Common Paths
- **Source**: `mobile/app/src/main/java/com/example/blackcar/`
- **Layouts**: `mobile/app/src/main/res/layout/`
- **Navigation**: `mobile/app/src/main/res/navigation/nav_graph.xml`
- **Build Config**: `mobile/app/build.gradle.kts`
- **Manifest**: `mobile/app/src/main/AndroidManifest.xml`

---
*Reference: Check Login, Profile, and History implementations for examples.*
