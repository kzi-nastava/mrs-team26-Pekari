# Mobile Development Guidelines - BlackCar Project

This document provides essential information for AI agents and developers working on the Android mobile module of the BlackCar project.

## Project Overview
BlackCar is a premium ride-hailing (Uber-like) application.
- **Backend**: Spring Boot API (`/api/v1` prefix).
- **Web**: Angular-based management/passenger interface.
- **Mobile**: Native Android application (Java).

## Architecture: MVVM + Repository Pattern
The project strictly follows the **MVVM (Model-View-ViewModel)** architectural pattern combined with the **Repository pattern**.

### 1. Presentation Layer
- **Views (Fragments/Activities)**: Use **ViewBinding**. Responsible for UI rendering and observing `LiveData` from ViewModels.
- **ViewModels**: Encapsulate UI logic. They interact with Repositories and expose `ViewState` objects.
- **ViewState**: Immutable classes (usually `abstract` with static subclasses like `Idle`, `Loading`, `Success`, `Error`) that represent the state of a specific screen.

### 2. Data Layer
- **Repositories**: The single source of truth for the app. They mediate between the `ViewModel` and the `ApiService`. Repositories handle error mapping (e.g., converting HTTP 401 to user-friendly messages).
- **API Services**: Retrofit interfaces defining network endpoints. **Do not implement these manually**; Retrofit generates implementations at runtime.
- **Models**: POJOs for JSON serialization/deserialization.

## Networking
- **Retrofit & OkHttp**: Used for all network requests.
- **ApiClient**: A singleton class located in `com.example.blackcar.data.api`. It configures the Retrofit instance, including Base URL, GSON converters, and logging interceptors.
- **Base URL**: Managed via `BuildConfig.API_BASE_URL` (configured in `app/build.gradle.kts`). For Android emulators, `10.0.2.2` points to the host machine.
- **Network Security**: `android:usesCleartextTraffic="true"` is enabled in `AndroidManifest.xml` to allow HTTP communication during development.

## Guidelines for Contributions
1. **Consistency**: Always look at the `web` module's services (e.g., `AuthService.ts`) for business logic inspiration. The mobile app should mirror the web app's behavior.
2. **Error Handling**: Perform error mapping in the Repository layer. Do not leak HTTP status codes or stack traces to the UI.
3. **Modular Code**: Keep fragments lean. Logic belongs in ViewModels, data fetching in Repositories.
4. **Navigation**: Use the Android Navigation Component. Define all actions in `res/navigation/nav_graph.xml`.
5. **Naming Conventions**:
   - Views: `FeatureFragment.java`
   - ViewModels: `FeatureViewModel.java`
   - States: `FeatureViewState.java`
   - Repositories: `FeatureRepository.java`

## Common Paths
- Source: `mobile/app/src/main/java/com/example/blackcar/`
- Layouts: `mobile/app/src/main/res/layout/`
- Navigation: `mobile/app/src/main/res/navigation/nav_graph.xml`
- Build Config: `mobile/app/build.gradle.kts`

---
*Note: This project is in active development. When in doubt, check existing implementations of Login and Profile features.*
