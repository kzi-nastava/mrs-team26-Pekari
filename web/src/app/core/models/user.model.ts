export interface User {
  id: string;
  email: string;
  username: string;
  firstName?: string;
  lastName?: string;
  role: 'admin' | 'passenger' | 'driver';
  blocked?: boolean;
}
