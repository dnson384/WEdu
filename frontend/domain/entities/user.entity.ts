export interface UserEntity {
  id: string;
  email: string;
  username: string;
  role: string;
  avatarUrl: string;
  accountType: string
}

export interface UserResponseEntity {
  id: string;
  email: string;
  role: string;
}

export interface ChangePasswordPayloadEntity {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}
