import axios from "axios";
import { LoginPayload, ResetPasswordPayload } from "../schemas/auth.schema";

import { AuthorizedResponseEntity } from "@/domain/entities/auth.entity";

export class AuthService {
  async login(payload: LoginPayload): Promise<AuthorizedResponseEntity> {
    const { data } = await axios.post<AuthorizedResponseEntity>(
      "/api/auth/login",
      payload,
    );
    return data;
  }

  async forgotPassword(email: string): Promise<string> {
    const { data } = await axios.post<string>(
      "/api/auth/forgot-password",
      {},
      { params: { email } },
    );

    return data;
  }

  async resetPassword(payload: ResetPasswordPayload): Promise<string> {
    const { data } = await axios.post<string>(
      "/api/auth/reset-password",
      payload,
    );

    return data;
  }
}
