import { UserResponseEntity } from "./user.entity";

export interface LoginPayloadEntity {
  email: string;
  plainPassword: string;
}

export interface RegisterPayloadEntity {
  email: string;
  plainPassword: string;
  confirmPassword: string;
  username: string;
  loginMethod: string;
}

export interface AuthorizedResponseEntity {
  user: UserResponseEntity;
  accessToken: string;
  refreshToken: string;
}
