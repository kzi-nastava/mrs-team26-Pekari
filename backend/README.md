# Blackcar Backend - Multi-Module Spring Boot Project

This project is a modularized Spring Boot application designed with a clear separation of concerns.

## Project Structure

The project is divided into three main Maven modules:

### 1. `blackcar-api`
- **Purpose**: Defines the contract and shared data structures.
- **Contents**: 
  - **DTOs (Data Transfer Objects)**: Request and Response models.
  - **Interfaces**: Service interfaces that define the business capabilities.
  - **Constants & Exceptions**: Shared constants and custom exception classes.
- **Dependencies**: Vanilla Java (No Spring Boot dependencies for lightweight integration).

### 2. `blackcar-core`
- **Purpose**: Contains the core business logic and data persistence.
- **Contents**:
  - **Service Implementations**: Logic that implements the `api` interfaces.
  - **Models/Entities**: Database domain models.
  - **Repositories**: Data access layer (JPA/Hibernate).
  - **Mappers**: Logic for converting between Entities and DTOs.
- **Dependencies**: Depends on `blackcar-api`.

### 3. `blackcar-web`
- **Purpose**: The entry point of the application and external communication layer.
- **Contents**:
  - **Controllers**: REST endpoints for web communication.
  - **Configurations**: Spring Boot configurations (Security, Swagger, etc.).
  - **Exception Handlers**: Global web exception handling.
  - **WebApplication.java**: The main Spring Boot application class.
- **Dependencies**: Depends on both `blackcar-api` and `blackcar-core`. Uses Spring Boot Starter Web.

---

## How to Run the Project

### Prerequisites
- Java 17 or higher
- Maven 3.6+

### Build the Project
To build all modules and install them to your local repository, run the following command from the `backend` directory:

```bash
mvn clean install
```

### Run the Application
You can run the application directly from the `blackcar-web` module using Maven:

```bash
mvn spring-boot:run -pl blackcar-web
```

Alternatively, you can run the `WebApplication.java` class from your IDE (IntelliJ IDEA).

---

## Development Workflow
When making changes to `api` or `core`, it is recommended to run `mvn install` on the root `backend` folder to ensure that the `web` module sees the latest changes in its dependencies.
