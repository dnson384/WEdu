import { UserEntity } from "@/domain/entities/user.entity";
import { IUserRepository } from "@/domain/repositories/IUserRepository";
import axios from "axios";
import { cookies } from "next/headers";

export class UserRepositoryImpl implements IUserRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  public async getMe(): Promise<UserEntity> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get(`${this.baseUrl}/user/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
      withCredentials: true,
    });
    return data;
  }

  public async uploadAvatar(formData: FormData): Promise<string> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.post<string>(
      `${this.baseUrl}/storage/avatar`,
      formData,
      {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${accessToken}`,
          "Content-Type": "multipart/form-data",
        },
      },
    );

    return data;
  }

  public async updateAvatar(s3Key: string): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/user/avatar`,
      { s3Key },
      {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  public async updateUsername(username: string): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/user/update-user`,
      { username },
      {
        withCredentials: true,
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }
}
