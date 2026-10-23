import { UserEntity } from "./user.entity";

export interface LoginPayloadEntity {
  email: string;
  plainPassword: string;
}

export interface NewUserRequestEntity {
  email: string;
  plainPassword: string;
  confirmPassword: string;
  username: string;
  loginMethod: string;
}

export interface AuthorizedResponseEntity {
  user: UserEntity;
  accessToken: string;
  refreshToken: string;
}

export interface ChangePasswordEntity {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}
