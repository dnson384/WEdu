import {
  LoginPayloadEntity,
  NewUserRequestEntity,
  AuthorizedResponseEntity,
} from "../entities/auth.entity";
import { ChangePasswordEntity } from "../entities/auth.entity";

export interface IAuthRepository {
  register(payload: NewUserRequestEntity): Promise<AuthorizedResponseEntity>;
  login(payload: LoginPayloadEntity): Promise<AuthorizedResponseEntity>;
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
