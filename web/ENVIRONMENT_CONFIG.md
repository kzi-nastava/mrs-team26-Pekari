# Environment Configuration Guide

## Overview
This guide explains the professional environment switching system implemented in the BlackCar Angular application.

## Architecture

### 1. **Environment Files** (`src/environments/`)
Three configuration files define settings for different deployment environments:

- **`environment.ts`** - Development (default for `ng serve`)
- **`environment.staging.ts`** - Staging environment
- **`environment.production.ts`** - Production environment

Each file exports an `EnvironmentConfig` object with:
- `production`: Boolean flag
- `apiUrl`: Backend API endpoint
- `appName`: Application display name
- `logLevel`: Logging level ('debug', 'info', 'warn', 'error')
- `enableDevTools`: Whether to show dev-only components

### 2. **EnvironmentService** (`src/app/core/services/environment.service.ts`)
Central service for accessing environment configuration throughout the app.

**Methods:**
- `getConfig()` - Get entire config object
- `getApiUrl()` - Get backend API URL
- `isProduction()` - Check if in production mode
- `areDevToolsEnabled()` - Check if dev tools are enabled
- `getLogLevel()` - Get log level setting
- `getAppName()` - Get app name

**Usage in components:**
```typescript
import { EnvironmentService } from './core/services/environment.service';

export class MyComponent {
  private env = inject(EnvironmentService);

  ngOnInit() {
    if (!this.env.isProduction()) {
      console.log('Development mode');
    }
    console.log('API URL:', this.env.getApiUrl());
  }
}
```

## Build Commands

### Development (default)
```bash
ng serve
# or explicitly
ng serve --configuration=development
```
Uses `environment.ts`

### Staging
```bash
ng serve --configuration=staging
# or for production build
ng build --configuration=staging
```
Uses `environment.staging.ts`

### Production
```bash
ng build --configuration=production
# or for prod serving (if needed)
ng serve --configuration=production
```
Uses `environment.production.ts`

## Why This Approach is Professional

1. **Type-Safe Configuration** - Strongly typed `EnvironmentConfig` interface
2. **Separation of Concerns** - Environment logic separated from business logic
3. **No Runtime Hacks** - Uses Angular's built-in file replacement mechanism
4. **Scalable** - Easy to add new environments or config properties
5. **CI/CD Friendly** - Integrates seamlessly with build pipelines
6. **Convention Over Configuration** - Follows Angular best practices
7. **Clear Intent** - `isProduction()` is much clearer than `isDevMode()`

## File Replacement Configuration

The `angular.json` uses `fileReplacements` to swap the environment file at build time:

```json
"fileReplacements": [
  {
    "replace": "src/environments/environment.ts",
    "with": "src/environments/environment.production.ts"
  }
]
```

This ensures the correct configuration is bundled for each environment.

## Best Practices

1. **Never hardcode API URLs** - Always use `environmentService.getApiUrl()`
2. **Keep config minimal** - Add only what's truly environment-specific
3. **Secure sensitive data** - Don't commit actual API keys; use placeholder values
4. **Update all environments** - When adding a new config property, update all three files
5. **Document changes** - Add comments when new properties are added

## Adding New Configuration Properties

1. Update the `EnvironmentConfig` interface in `environment.service.ts`
2. Add the property to all three environment files
3. Add a getter method to `EnvironmentService`
4. Use via dependency injection in components

Example:
```typescript
// In environment.service.ts interface
export interface EnvironmentConfig {
  // ... existing properties
  featureFlags: {
    enableNewUI: boolean;
  };
}

// Add getter
getFeatureFlags() {
  return this.config.featureFlags;
}
```

## Migration Notes

- Removed dependency on Angular's `isDevMode()` 
- Replaced with explicit environment configuration
- Dev helper component now controlled via `environment.ts` property
