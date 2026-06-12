export type Role = 'VIEWER' | 'MEMBER' | 'ADMIN' | 'OWNER';

export interface AuthUser {
  userId: string;
  tenantId: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  fullName: string;
  email: string;
  password: string;
  organizationName: string;
}

export interface RefreshRequest {
  refreshToken: string;
}

export interface AuthResponse {
  access_token: string;
  refresh_token: string;
}
