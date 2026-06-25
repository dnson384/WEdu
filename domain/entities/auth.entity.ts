import { UserResponseEntity } from "./user.entity";

export interface LoginPayloadEntity {
  email: string;
  plainPassword: string;
}

export interface AuthorizedResponseEntity {
  user: UserResponseEntity;
  accessToken: string;
  refreshToken: string;
}
