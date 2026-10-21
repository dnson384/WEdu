import {
  AuthorizedResponseEntity,
  LoginPayloadEntity,
  RegisterPayloadEntity,
} from "../entities/auth.entity";

export interface IAuthRepository {
  login(payload: LoginPayloadEntity): Promise<AuthorizedResponseEntity>;
  register(payload: RegisterPayloadEntity): Promise<AuthorizedResponseEntity>;
  logout(): Promise<boolean>;
  forgotPassword(email: string): Promise<string>;
  resetPassword(
    plainToken: string,
    newPassword: string,
    confirmNewPassword: string,
  ): Promise<string>;
}
