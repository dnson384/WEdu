import { UserEntity } from "./user.entity";

export interface VerificationEntity {
  email: string;
  expiresAt: string;
  otpExpiresAt: string;
  resendAt: string;
  remainingAttempts: number;
  serverTime: string;
}

// Server-to-server transport only. Cookies never form part of a browser JSON response.
export interface AuthFlowResult {
  status: number;
  data: VerificationEntity | UserEntity;
  cookies: string[];
}
