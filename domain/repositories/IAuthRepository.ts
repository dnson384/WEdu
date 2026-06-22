import { AuthorizedResponseEntity, LoginPayloadEntity } from "../entities/auth.entity";

export interface IAuthRepository {
  login(payload: LoginPayloadEntity): Promise<AuthorizedResponseEntity>;
  // Register(): Promise<AuthorizedResponseEntity>;
}
