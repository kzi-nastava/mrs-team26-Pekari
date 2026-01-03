# BlackCar Backend - AI Agent Context Guide

**Project**: Multi-module Spring Boot ride-sharing application
**Tech Stack**: Java 17, Spring Boot 3.4.1, PostgreSQL, Maven
**Architecture**: Modular 3-tier architecture with strict dependency management

---

##  Module Architecture

### Dependency Flow
```
blackcar-web (runtime) → blackcar-core → blackcar-api
```

### Module Details

#### 1️⃣ **blackcar-api** (Contract Layer)
- **Purpose**: Pure API definitions - NO implementations
- **Contains**:
  - Service interfaces (e.g., `AuthService`, `RideService`)
  - Request/Response DTOs (e.g., `RegisterUserRequest`, `AuthResponse`)
  - Custom exceptions (e.g., `DuplicateResourceException`, `InvalidTokenException`)
- **Dependencies**: NONE - Pure Java with Lombok only
- **Key Rule**: NO Spring dependencies, NO `MultipartFile`, use `byte[]` for files

#### 2️⃣ **blackcar-core** (Business Logic Layer)
- **Purpose**: Service implementations and data persistence
- **Contains**:
  - Service implementations (e.g., `AuthServiceImpl implements AuthService`)
  - JPA Entities/Models (e.g., `User`, `Driver`, `AccountActivationToken`)
  - Repositories (e.g., `UserRepository`, `AccountActivationTokenRepository`)
  - Configuration (Security, JWT, Mail)
- **Dependencies**: `blackcar-api`, Spring Boot Starter, Spring Data JPA, Spring Security, JWT (jjwt 0.12.3)
- **Key Rule**: Only implements interfaces from `blackcar-api`

#### 3️⃣ **blackcar-web** (Presentation Layer)
- **Purpose**: REST API endpoints and web configurations
- **Contains**:
  - Controllers (e.g., `AuthController`, `RideController`)
  - **Web-specific DTOs with `Web` prefix** (e.g., `WebRegisterUserRequest`, `WebAuthResponse`)
  - Mappers between Web DTOs and API DTOs (e.g., `AuthMapper`)
  - Global exception handlers
  - Swagger/OpenAPI configuration
  - Main application class: `WebApplication.java`
- **Dependencies**: `blackcar-api` (compile), `blackcar-core` (runtime), Spring Boot Web, Spring Security, Validation
- **Key Rule**: `blackcar-core` is **runtime scope only** - strict API encapsulation

