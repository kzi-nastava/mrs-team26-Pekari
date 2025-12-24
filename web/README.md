# Developer README - Web Application

This project is a frontend application built with Angular.

## Structure

The project follows a feature-based structure:

- `src/app/shared/`: Contains components, pipes, and directives shared across different features.
  - `components/`: Generic reusable components (e.g., Header, Footer).
- `src/app/features/`: Contains feature components, organized by domain or page.
  - Each subfolder represents a logical feature or a specific page (e.g., `home`, `products`, `auth`).
  - Inside a feature folder, you can have its own components, services, or models if they are only used within that feature.
  - Example: `src/app/features/home/` contains the `home.component.ts` and its related files.
- `src/app/app.routes.ts`: Centralized routing configuration.
- `src/app/app.ts`: Root component of the application.

## Technologies and Versions

- **Angular**: ^21.0.0
- **Node.js**: 24.11.0
- **Package Manager**: npm 11.6.1
- **TypeScript**: ~5.9.2
- **Testing**: Vitest

## Build and Development

### Development Server
Run `npm start` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

### Build
Run `npm run build` to build the project. The build artifacts will be stored in the `dist/` directory.

### Running Tests
Run `npm test` to execute the unit tests via [Vitest](https://vitest.dev/).
