export interface UserEntity {
  id: string;
  email: string;
  username: string;
  role: string;
  avatarUrl: string;
  isActived: boolean;
  accountType: string;
  plusExpiresAt?: string | null;
}

export interface UserResponseEntity {
  id: string;
  email: string;
  role: string;
}