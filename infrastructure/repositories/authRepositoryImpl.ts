import {
  AuthorizedResponseEntity,
  LoginPayloadEntity,
} from "@/domain/entities/auth.entity";
import { IAuthRepository } from "@/domain/repositories/IAuthRepository";
import axios from "axios";
import { cookies } from "next/headers";

interface RawResponse {
  id: string;
  email: string;
  role: string;
}

export class authRepositoryImpl implements IAuthRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  public async login(payload: LoginPayloadEntity): Promise<AuthorizedResponseEntity> {
    const response = await axios.post(`${this.baseUrl}/user/login`, payload, {
      withCredentials: true,
    });

    const authorizedResponse: AuthorizedResponseEntity = {
      user: response.data,
      accessToken: "",
      refreshToken: "",
    };

    if (typeof response.headers.getSetCookie === "function") {
      const cookies: string[] = response.headers.getSetCookie();

      const accessToken = cookies[0].split(";")[0].split("=")[1];
      const refreshToken = cookies[1].split(";")[0].split("=")[1];

      authorizedResponse.accessToken = accessToken;
      authorizedResponse.refreshToken = refreshToken;
    }
    return authorizedResponse;
  }

  public async logout(): Promise<boolean> {
    const cookieStore = await cookies();

    const accessToken = cookieStore.get("accessToken")?.value;
    const refreshToken = cookieStore.get("refreshToken")?.value;

    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/user/logout`,
      {
        refreshToken,
      },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );

    return data;
  }
}
