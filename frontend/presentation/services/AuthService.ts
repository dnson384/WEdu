import axios from "axios";
import {
  ChangePasswordPayload,
  LoginPayload,
  RegisterPayload,
  ResetPasswordPayload,
} from "../schemas/auth.schema";
import { AuthorizedResponseEntity } from "@/domain/entities/auth.entity";

export class AuthService {
  constructor() {}

  async register(payload: RegisterPayload) {
    const { data } = await axios.post<AuthorizedResponseEntity>(
      "/api/auth/register",
      payload,
    );
    return data;
  }

  async login(payload: LoginPayload): Promise<AuthorizedResponseEntity> {
    const { data } = await axios.post<AuthorizedResponseEntity>(
      "/api/auth/login",
      payload,
    );
    return data;
  }

  async logout(): Promise<boolean> {
    const { data } = await axios.post("/api/auth/logout");
    return data;
  }

  async changePassword(payload: ChangePasswordPayload): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      "/api/auth/change-password",
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
