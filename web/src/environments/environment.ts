// Development environment
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api/v1',
  appName: 'BlackCar (Dev)',
  logLevel: 'debug' as const,
  enableDevTools: true,
  firebase: {
    // Fill these from your Firebase Web App settings (optional for web push usage)
    apiKey: 'AIzaSyBr-TRiFIIMD5TklMAEjNl8H1Cds4Ds4nU',
    authDomain: 'blackcar-5b038.firebaseapp.com',
    projectId: 'blackcar-5b038',
    messagingSenderId: '181429850748',
    appId: '1:181429850748:android:f9684719748cca102d6ff9',
    vapidKey: '' // Web Push certificates (VAPID public key)
  }
} as const;
