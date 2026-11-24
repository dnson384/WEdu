import { UserEntity } from "@/domain/entities/user.entity";
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

  public async getMe(accessToken: string): Promise<UserEntity> {
    const { data } = await axios.get(`${this.baseUrl}/user/me`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });
    return data;
  }

  public async updateAvatar(
    s3Key: string,
    accessToken: string,
  ): Promise<boolean> {
    const { data } = await axios.patch(
      `${this.baseUrl}/user/avatar/${s3Key}`,
      {},
      {
        params: {
          s3Key: s3Key,
        },
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  public async updateUsername(
    username: string,
    accessToken: string,
  ): Promise<boolean> {
    const { data } = await axios.patch(
      `${this.baseUrl}/user/update-username`,
      {},
      {
        params: {
          username: username,
        },
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }
}
