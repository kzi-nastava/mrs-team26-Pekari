export interface ProfileData {
  id: string;
  email: string;
  username: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  role: 'admin' | 'passenger' | 'driver';
  profilePicture?: string; // Base64 or URL
  createdAt: Date;
  updatedAt: Date;
}

export interface DriverInfo {
  hoursActiveLast24h: number;
  vehicle: VehicleInfo;
}

export interface VehicleInfo {
  id: string;
  make: string;
  model: string;
  year: number;
  licensePlate: string;
  vin: string;
}

export interface ProfileUpdateRequest {
  firstName: string;
  lastName: string;
  phoneNumber: string;
  address: string;
  profilePicture?: string;
  vehicle?: VehicleInfo; // Only for drivers
}

export interface ApprovalRequest {
  id: string;
  userId: string;
  changes: ProfileUpdateRequest;
  status: 'pending' | 'approved' | 'rejected';
  createdAt: Date;
  reviewedAt?: Date;
  reviewedBy?: string;
  rejectionReason?: string;
}

export interface PasswordChangeRequest {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
}
