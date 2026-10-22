import { UserEntity } from "@/domain/entities/user.entity";
import { IAdminRepository } from "@/domain/repositories/IAdminRepository";
import axios from "axios";

export class AdminRepositoryImpl implements IAdminRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async getAllUsers(accessToken: string): Promise<UserEntity[]> {
    const { data } = await axios.get(`${this.baseUrl}/admin/users`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    return data;
  }

  async findUsers(keyword: string, accessToken: string): Promise<UserEntity[]> {
    const { data } = await axios.get(`${this.baseUrl}/admin/users/search`, {
      params: {
        keyword: keyword,
      },
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    return data;
  }

  async updateUserRole(
    userId: string,
    role: string,
    accessToken: string,
  ): Promise<UserEntity> {
    const { data } = await axios.put(
      `${this.baseUrl}/admin/${userId}/role`,
      {},
      {
        params: {
          role: role,
        },
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async lockUser(userId: string, accessToken: string): Promise<UserEntity> {
    const { data } = await axios.put(
      `${this.baseUrl}/admin/${userId}/lock`,
      {},
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async unlockUser(userId: string, accessToken: string): Promise<UserEntity> {
    const { data } = await axios.put(
      `${this.baseUrl}/admin/${userId}/unlock`,
      {},
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async deleteUser(userId: string, accessToken: string): Promise<boolean> {
    const { data } = await axios.delete(`${this.baseUrl}/admin/${userId}`, {
      headers: {
        Authorization: `Bearer ${accessToken}`,
      },
    });

    return data;
  }
}
