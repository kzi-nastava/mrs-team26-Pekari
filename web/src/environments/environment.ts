// Development environment
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  appName: 'BlackCar (Dev)',
  logLevel: 'debug' as const,
  enableDevTools: true
} as const;
