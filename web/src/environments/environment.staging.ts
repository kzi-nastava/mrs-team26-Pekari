// Staging environment
export const environment = {
  production: false,
  apiUrl: 'https://staging-api.blackcar.com/api',
  appName: 'BlackCar (Staging)',
  logLevel: 'info' as const,
  enableDevTools: false
} as const;
