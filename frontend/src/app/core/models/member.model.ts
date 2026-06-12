import { Role } from './auth.model';

export interface Member {
  user_id: string;
  full_name: string;
  email: string;
  role: Role;
  joined_at: string;
}

export interface UpdateRoleRequest {
  role: Role;
}
