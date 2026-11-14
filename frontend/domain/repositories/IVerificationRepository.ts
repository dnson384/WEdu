import { AuthFlowResult } from "../entities/verification.entity";
import { NewUserRequestEntity } from "../entities/auth.entity";

export interface IVerificationRepository {
  register(payload: NewUserRequestEntity): Promise<AuthFlowResult>;
  metadata(token?: string): Promise<AuthFlowResult>;
  verify(otp: string, token?: string): Promise<AuthFlowResult>;
  resend(token?: string): Promise<AuthFlowResult>;
  exchange(code: string, verifier: string): Promise<AuthFlowResult>;
}
