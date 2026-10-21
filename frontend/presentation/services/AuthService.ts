import axios from "axios";
import { ResetPasswordPayload } from "../schemas/auth.schema";

export class AuthService {
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
