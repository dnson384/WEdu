export interface UserEntity {
  id: string, 
  email: string,
  username: string,
  role: string,
  avatarUrl: string,
}

export interface UserResponseEntity {
  id: string;
  email: string;
  role: string;
}
