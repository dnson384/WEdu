import axios from "axios";
import { AuthFlowResult } from "@/domain/entities/verification.entity";
import { NewUserRequestEntity } from "@/domain/entities/auth.entity";
import { IVerificationRepository } from "@/domain/repositories/IVerificationRepository";

export class VerificationRepositoryImpl implements IVerificationRepository {
  private async request(
    path: string,
    method: "GET" | "POST",
    data?: unknown,
    token?: string,
  ): Promise<AuthFlowResult> {
    const backendUrl = process.env.BACKEND_URL?.replace(/\/+$/, "");
    if (!backendUrl) throw new Error("BACKEND_URL is required");
    const response = await axios.request({
      url: `${backendUrl}/auth/${path}`,
      method,
      data,
      timeout: 35000,
      headers:
        token && /^[a-f0-9]{64}$/.test(token)
          ? { Cookie: `verificationToken=${token}` }
          : {},
    });
    return {
      status: response.status,
      data: response.data,
      cookies: response.headers["set-cookie"] ?? [],
    };
  }

  register(payload: NewUserRequestEntity) {
    return this.request("register", "POST", payload);
  }
  metadata(token?: string) {
    return this.request("verification", "GET", undefined, token);
  }
  verify(otp: string, token?: string) {
    return this.request("verification/verify", "POST", { otp }, token);
  }
  resend(token?: string) {
    return this.request("verification/resend", "POST", {}, token);
  }
  exchange(code: string, verifier: string) {
    return this.request("google/exchange", "POST", { code, verifier });
  }
}
