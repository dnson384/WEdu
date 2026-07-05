import axios from "axios";
import { LoginPayload, RegisterPayload } from "../schemas/auth.schema";
import { AuthorizedResponseEntity } from "@/domain/entities/auth.entity";

export async function RegisterService(payload: RegisterPayload) {
  const { data } = await axios.post<AuthorizedResponseEntity>(
    "/api/auth/register",
    payload,
  );
  return data;
}

export async function LoginService(
  payload: LoginPayload,
): Promise<AuthorizedResponseEntity> {
  const response = await axios.post("/api/auth/login", payload);
  return response.data;
}

export async function LogoutService(): Promise<boolean> {
  const { data } = await axios.post("/api/auth/logout");
  return data;
}
