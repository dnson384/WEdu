import { ChangePasswordEntity } from "../entities/auth.entity";
import {
  AuthorizedResponseEntity,
  LoginPayloadEntity,
  NewUserRequestEntity,
} from "../entities/auth.entity";

export interface IAuthRepository {
  login(payload: LoginPayloadEntity): Promise<AuthorizedResponseEntity>;
  register(payload: NewUserRequestEntity): Promise<AuthorizedResponseEntity>;
  logout(accessToken: string): Promise<boolean>;
  changePassword(
    payload: ChangePasswordEntity,
    accessToken: string,
  ): Promise<boolean>;
  forgotPassword(email: string): Promise<string>;
  resetPassword(
    plainToken: string,
    newPassword: string,
    confirmNewPassword: string,
  ): Promise<string>;
}
