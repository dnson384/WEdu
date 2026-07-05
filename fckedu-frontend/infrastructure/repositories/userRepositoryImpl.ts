import {
  ChangePasswordPayloadEntity,
  UserEntity,
} from "@/domain/entities/user.entity";
import { IUserRepository } from "@/domain/repositories/IUserRepository";
import axios from "axios";

export class UserRepositoryImpl implements IUserRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  public async getMe(
    accessToken: string,
    refreshToken: string,
  ): Promise<UserEntity> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get(`${this.baseUrl}/user/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
        Cookie: customCookieHeader,
      },
    });
    return data;
  }

  public async uploadAvatar(
    formData: FormData,
    accessToken: string,
    refreshToken: string,
  ): Promise<string> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.post<string>(
      `${this.baseUrl}/storage/avatar`,
      formData,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
          "Content-Type": "multipart/form-data",
        },
      },
    );

    return data;
  }

  public async updateAvatar(
    s3Key: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/user/avatar`,
      { s3Key },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  public async updateUsername(
    username: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/user/update-user`,
      { username },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  public async changePassword(
    payload: ChangePasswordPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/user/change-password`,
      { payload, refreshToken },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  public async lockAccount(
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/user/lock`,
      { refreshToken },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }
  public async deleteAccount(
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.delete<boolean>(
      `${this.baseUrl}/user/delete`,
      {
        headers: {
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }
}
