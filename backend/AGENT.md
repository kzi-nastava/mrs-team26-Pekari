# BlackCar Backend - AI Agent Context Guide

**Project**: Multi-module Spring Boot ride-sharing application (taxi/uber-like service)
**Tech Stack**: Java 17, Spring Boot 3.4.1, PostgreSQL, Maven
**Architecture**: Modular 3-tier architecture with strict dependency management
**Domain**: Ride-sharing platform with passengers, drivers, real-time ride ordering, driver matching

---

## 🏗️ Module Architecture

### Dependency Flow
```
blackcar-web (runtime) → blackcar-core → blackcar-api
```

### Module Details

#### 1️⃣ **blackcar-api** (Contract Layer)
- **Purpose**: Pure API definitions - NO implementations
- **Location**: `backend/blackcar-api/src/main/java/com/pekara/`
- **Contains**:
  - Service interfaces: `AuthService`, `RideService`, `DriverStateService`, `JwtService`
  - Request DTOs: `RegisterUserRequest`, `RegisterDriverRequest`, `OrderRideRequest`, `EstimateRideRequest`, `UpdateDriverLocationRequest`, `UpdateDriverOnlineStatusRequest`
  - Response DTOs: `AuthResponse`, `RideEstimateResponse`, `OrderRideResponse`, `RegisterDriverResponse`, `DriverStateResponse`
  - Common DTOs: `LocationPointDto` (latitude, longitude, address)
  - Custom exceptions: `DuplicateResourceException`, `InvalidTokenException`, `NoDriversAvailableException`, `NoActiveDriversException`, `InvalidScheduleTimeException`
  - Enums: `RideStatus` (PENDING, ACCEPTED, IN_PROGRESS, COMPLETED, CANCELLED)
- **Dependencies**: NONE - Pure Java with Lombok only
- **Key Rule**: NO Spring dependencies, NO `MultipartFile`, use `byte[]` for files

#### 2️⃣ **blackcar-core** (Business Logic Layer)
- **Purpose**: Service implementations and data persistence
- **Location**: `backend/blackcar-core/src/main/java/com/pekara/`
- **Contains**:
  - **Service Implementations**:
    - `AuthServiceImpl` - User/Driver registration, login, account activation via email tokens, password reset
    - `RideServiceImpl` - Ride estimation (pricing, distance, duration), ride ordering, driver matching algorithm
    - `DriverStateServiceImpl` - Driver location updates, online/offline status, busy state management
    - `JwtServiceImpl` - JWT token generation/validation using HS256 and jjwt library
    - `MailServiceImpl` - Email sending (activation, password reset, ride reminders) via Spring Mail
    - `RideReminderScheduler` - Scheduled task for sending 15-minute reminders before scheduled rides
  - **JPA Entities** (`com.pekara.model`):
    - `User` - Base entity (single-table inheritance): email, password, firstName, lastName, phoneNumber, address, profilePicture, role (enum: PASSENGER/DRIVER/ADMIN), isActive, totalRides, averageRating
    - `Driver extends User` - Additional fields: licenseNumber, licenseExpiry, vehicleRegistration, vehicleType
    - `Ride` - Main ride entity: creator (User), driver (Driver), passengers (ManyToMany Set<User>), stops (OneToMany List<RideStop>), status (enum), vehicleType, babyTransport, petTransport, scheduledAt, estimatedPrice, distanceKm, estimatedDurationMinutes, startedAt, completedAt, lastReminderSentAt
    - `RideStop` - Waypoints for rides: ride (ManyToOne), sequenceIndex, latitude, longitude, address
    - `DriverState` - Real-time driver state (OneToOne with Driver): online, busy, latitude, longitude, currentRideEndsAt, currentRideEndLatitude/Longitude, nextScheduledRideAt, updatedAt, version (optimistic locking)
    - `DriverWorkLog` - Driver work session tracking
    - `AccountActivationToken` - Email activation tokens with 24h expiration
  - **Repositories** (`com.pekara.repository`):
    - Spring Data JPA repositories: `UserRepository`, `DriverRepository`, `RideRepository`, `DriverStateRepository`, `AccountActivationTokenRepository`, `DriverWorkLogRepository`
  - **Configuration**:
    - `CoreSecurityConfig` - Password encoder (BCrypt), Authentication manager
    - `CoreDevDataSeeder` - Dev environment data seeding (users, drivers, rides)
  - **Utilities**:
    - `GeoUtils` - Haversine distance calculation (lat/lon to kilometers)
- **Dependencies**: `blackcar-api`, Spring Boot Starter, Spring Data JPA, Spring Security, JWT (jjwt 0.12.3), Spring Mail
- **Key Rule**: Only implements interfaces from `blackcar-api`

#### 3️⃣ **blackcar-web** (Presentation Layer)
- **Purpose**: REST API endpoints and web configurations
- **Location**: `backend/blackcar-web/src/main/java/com/pekara/`
- **Contains**:
  - **Controllers** (`com.pekara.controller`):
    - `AuthController` - `/api/v1/auth/*` - login, register (user/driver), activate, reset-password
    - `RideController` - `/api/v1/rides/*` - estimate, order, history, tracking, cancel, rate
    - `DriverStateController` - `/api/v1/drivers/state/*` - update location, update online status, get state
    - `ProfileController` - `/api/v1/profile/*` - get/update user profile
    - `VehicleController` - `/api/v1/vehicles/*` - vehicle registration, get available vehicles
  - **Web DTOs** (`com.pekara.dto.*`) - All prefixed with `Web`:
    - Requests: `WebRegisterUserRequest`, `WebRegisterDriverRequest`, `WebLoginRequest`, `WebOrderRideRequest`, `WebEstimateRideRequest`, `WebCancelRideRequest`, `WebRideRatingRequest`, `WebUpdateProfileRequest`, `WebResetPasswordRequest`, `WebNewPasswordRequest`, `WebVehicleRegistrationRequest`, `WebRideHistoryFilterRequest`, `WebInconsistencyReportRequest`
    - Responses: `WebAuthResponse`, `WebRideEstimateResponse`, `WebOrderRideResponse`, `WebRegisterResponse`, `WebRegisterDriverResponse`, `WebMessageResponse`, `WebErrorResponse`, `WebDriverProfileResponse`, `WebPassengerProfileResponse`, `WebRideDetailResponse`, `WebRideTrackingResponse`, `WebDriverRideHistoryResponse`, `WebPassengerRideHistoryResponse`, `WebPaginatedResponse`, `WebActiveVehicleResponse`, `WebVehicleResponse`, `WebFavouriteRouteResponse`
    - Common: `WebLocationPoint`
  - **Mappers** (`com.pekara.mapper`):
    - `AuthMapper`, `RideMapper` - Convert between Web DTOs and API DTOs (includes `MultipartFile` to `byte[]` conversion)
  - **Exception Handling**:
    - `GlobalExceptionHandler` - Centralized exception handling, returns `WebErrorResponse` with HTTP status codes
  - **Security Configuration**:
    - `SecurityConfig` - JWT filter, CORS config (reads `cors.allowed.origins` from env), public endpoints: `/api/v1/auth/**`, `/api/v1/rides/estimate`, `/api/v1/vehicles`, `/swagger-ui/**`, `/v3/api-docs/**`
    - `JwtAuthFilter` - Extracts JWT from Authorization header, validates, sets SecurityContext
  - **Other Config**:
    - `WebConfig` - Web MVC configuration
    - `OpenApiConfig` - Swagger/OpenAPI configuration
    - `DevDataSeeder` - Additional web-layer dev data seeding
  - **Main Application**: `WebApplication.java` - Spring Boot entry point
- **Dependencies**: `blackcar-api` (compile), `blackcar-core` (runtime), Spring Boot Web, Spring Security, Spring Validation, Springdoc OpenAPI
- **Key Rule**: `blackcar-core` is **runtime scope only** - strict API encapsulation

---

## 🔑 Key Domain Concepts

### Ride Lifecycle
1. **PENDING** - Ride created, waiting for driver assignment
2. **ACCEPTED** - Driver assigned, preparing to pick up
3. **IN_PROGRESS** - Driver picked up passenger, en route to destination
4. **COMPLETED** - Ride finished successfully
5. **CANCELLED** - Ride cancelled by passenger or driver

### Ride Types
- **Immediate Rides** - scheduledAt is null, matched to nearest available driver immediately
- **Scheduled Rides** - scheduledAt is set (max 5 hours in future), system sends 15-min reminder email

### Driver Matching Algorithm
Located in `RideServiceImpl.orderRide()`:
- For immediate rides: Finds online, non-busy drivers and selects closest one using Haversine distance
- For scheduled rides: Finds drivers who will be free at scheduled time (considers currentRideEndsAt and nextScheduledRideAt)
- Throws `NoDriversAvailableException` if no suitable driver found

### Pricing Model
- Base rate: **120 RSD per kilometer** (defined in `RideServiceImpl.PRICE_PER_KM`)
- Distance calculated using Haversine formula (straight-line distance)
- Duration estimated: 2 min/km for driving + 3 min per additional stop

### Real-time Driver State
`DriverState` entity tracks:
- **online/busy** - Current availability
- **latitude/longitude** - Current GPS location
- **currentRideEndsAt** + end location - When current ride finishes
- **nextScheduledRideAt** - Next scheduled commitment
- Uses optimistic locking (@Version) for concurrent updates

### Security & Authentication
- **JWT Tokens**: HS256 algorithm, configurable expiration (default 24h), stored secret in env var `JWT_SECRET`
- **Password**: BCrypt hashing
- **Account Activation**: Email-based with 24h expiration tokens
- **Roles**: PASSENGER, DRIVER, ADMIN (enum `UserRole`)
- **Protected Endpoints**: All except `/api/v1/auth/**`, `/api/v1/rides/estimate`, `/api/v1/vehicles`, Swagger UI

---

## 🔧 Environment Configuration

Required environment variables (`.env` file):

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/blackcar
DB_USERNAME=postgres
DB_PASSWORD=your_password

# JWT
JWT_SECRET=your_jwt_secret_key_at_least_256_bits
JWT_EXPIRATION=86400000  # 24 hours in ms

# Server
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:4200

# Email (Gmail example)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
MAIL_FROM=noreply@blackcar.com
FRONTEND_URL=http://localhost:4200
```

---

## 📋 Common Development Patterns

### Adding a New Feature
1. **Define contract in `blackcar-api`**:
   - Create service interface in `com.pekara.service`
   - Add request/response DTOs in `com.pekara.dto.request/response`
   - Add custom exceptions if needed in `com.pekara.exception`

2. **Implement business logic in `blackcar-core`**:
   - Create `ServiceImpl` implementing the interface
   - Add entities in `com.pekara.model` if needed
   - Create repositories extending `JpaRepository`
   - Add configuration beans if required

3. **Expose via REST in `blackcar-web`**:
   - Create controller in `com.pekara.controller`
   - Create Web DTOs prefixed with `Web` in `com.pekara.dto`
   - Add mapper methods to convert Web DTOs ↔ API DTOs
   - Update `SecurityConfig` if endpoint needs special access rules
   - Document with Swagger annotations

### Working with DTOs
- **API DTOs** (`blackcar-api`): Used between modules, NO Spring types
- **Web DTOs** (`blackcar-web`): Used in HTTP layer, can include `MultipartFile`, validation annotations
- **Mapping**: Use mapper classes to convert between layers

### Database Changes
- Entities are auto-managed by Hibernate (Spring Data JPA)
- For schema updates, consider using Flyway/Liquibase (not yet implemented)
- Dev data seeding: `CoreDevDataSeeder` (runs on startup in dev profile)

### Testing Strategy
- Unit tests: Mock dependencies, test business logic in isolation
- Integration tests: Use `@SpringBootTest`, test full stack with embedded DB
- Test files located in `src/test/java` for each module

---

## 🚨 Important Rules & Gotchas

### Module Boundaries
❌ **NEVER** do this:
- Import Spring types in `blackcar-api`
- Use `blackcar-core` classes directly in `blackcar-web` (use interfaces only)
- Put business logic in controllers
- Return entities directly from REST endpoints

✅ **ALWAYS** do this:
- Define contracts first in `blackcar-api`
- Keep DTOs pure and separate by layer
- Use mappers to convert between layers
- Implement interfaces in `blackcar-core`
- Controllers should be thin - delegate to services

### Naming Conventions
- **API DTOs**: Plain names (e.g., `RegisterUserRequest`, `AuthResponse`)
- **Web DTOs**: `Web` prefix (e.g., `WebRegisterUserRequest`, `WebAuthResponse`)
- **Entities**: No suffix (e.g., `User`, `Ride`, `Driver`)
- **Services**: Interface in `api`, implementation with `Impl` suffix in `core`
- **Controllers**: `Controller` suffix (e.g., `AuthController`)

### Location Handling
- Use `LocationPointDto` (in API) with latitude, longitude, address
- Use `WebLocationPoint` (in Web) for HTTP requests/responses
- Always validate coordinates: lat [-90, 90], lon [-180, 180]
- Distance calculations use `GeoUtils.haversineKm()`

### Security Notes
- JWT secret MUST be at least 256 bits (32 characters) for HS256
- Never log passwords or tokens
- Account must be activated (`isActive=true`) before login
- Driver registration creates inactive account, admin must approve (not yet implemented)

---

## 🏃 Running the Application

```bash
# From backend directory
cd backend

# Build all modules
mvn clean install

# Run application (from blackcar-web)
cd blackcar-web
mvn spring-boot:run

# Or run the JAR
java -jar blackcar-web/target/blackcar-web-1.0-SNAPSHOT.jar
```

**Access Points**:
- API: `http://localhost:8080/api/v1`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- API Docs: `http://localhost:8080/v3/api-docs`

---

## 📚 Useful Commands

```bash
# Run tests for specific module
mvn test -pl blackcar-core

# Build without tests
mvn clean install -DskipTests

# View dependency tree
mvn dependency:tree

# Format code (if configured)
mvn spring-javaformat:apply
```

---

## 🐛 Debugging Tips

### Database Issues
- Check PostgreSQL is running: `psql -U postgres -d blackcar`
- Verify `.env` file exists and has correct DB credentials
- Check Hibernate logs: set `logging.level.org.hibernate.SQL=DEBUG` in application.properties

### JWT Issues
- Verify `JWT_SECRET` is at least 256 bits
- Check token expiration: default is 24h (86400000 ms)
- Frontend must send token as: `Authorization: Bearer <token>`

### Driver Matching Fails
- Verify drivers exist in DB and have `DriverState` records
- Check drivers are online: `DriverState.online = true`
- Check drivers are not busy: `DriverState.busy = false`
- Verify driver locations are set (latitude/longitude not null)

### Email Not Sending
- Gmail requires "App Password" (not regular password)
- Check firewall allows outbound SMTP (port 587)
- Verify `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM` in `.env`

---

## 🔍 Code Locations Quick Reference

| Component | Module | Package | Example Class |
|-----------|--------|---------|---------------|
| Service Interfaces | `blackcar-api` | `com.pekara.service` | `RideService.java` |
| DTOs (API) | `blackcar-api` | `com.pekara.dto.request/response` | `OrderRideRequest.java` |
| Exceptions | `blackcar-api` | `com.pekara.exception` | `NoDriversAvailableException.java` |
| Service Implementations | `blackcar-core` | `com.pekara.service` | `RideServiceImpl.java` |
| Entities | `blackcar-core` | `com.pekara.model` | `Ride.java`, `Driver.java` |
| Repositories | `blackcar-core` | `com.pekara.repository` | `RideRepository.java` |
| Utilities | `blackcar-core` | `com.pekara.util` | `GeoUtils.java` |
| Controllers | `blackcar-web` | `com.pekara.controller` | `RideController.java` |
| Web DTOs | `blackcar-web` | `com.pekara.dto` | `WebOrderRideRequest.java` |
| Mappers | `blackcar-web` | `com.pekara.mapper` | `RideMapper.java` |
| Security | `blackcar-web` | `com.pekara.config`, `com.pekara.security` | `SecurityConfig.java`, `JwtAuthFilter.java` |

---

**Last Updated**: 2026-01-25
**Version**: 1.1

