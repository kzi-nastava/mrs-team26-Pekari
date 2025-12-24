// Production environment
export const environment = {
  production: true,
  apiUrl: 'https://api.blackcar.com/api',
  appName: 'BlackCar',
  logLevel: 'error' as const,
  enableDevTools: false
} as const;
