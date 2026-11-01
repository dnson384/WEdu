import { AdminUserEntity } from "@/domain/entities/adminUser.entity";
import axios from "axios";

export class AdminService {
  constructor() {}

  async getAllUsers(signal?: AbortSignal): Promise<AdminUserEntity[]> {
    const { data } = await axios.get<AdminUserEntity[]>(`/api/admin/users`, {
      signal,
    });
    return data;
  }

  async findUsers(
    keyword: string,
    signal?: AbortSignal,
  ): Promise<AdminUserEntity[]> {
    const { data } = await axios.get<AdminUserEntity[]>(
      `/api/admin/users/search`,
      {
        signal,
        params: {
          keyword,
        },
      },
    );
    return data;
  }

  async updateUserRole(userId: string, role: string): Promise<AdminUserEntity> {
    const { data } = await axios.put<AdminUserEntity>(
      `/api/admin/${userId}/role`,
      {},
      { params: { role } },
    );
    return data;
  }

  async lockUser(userId: string): Promise<AdminUserEntity> {
    const { data } = await axios.put<AdminUserEntity>(
      `/api/admin/${userId}/lock`,
    );
    return data;
  }

  async unLockUser(userId: string): Promise<AdminUserEntity> {
    const { data } = await axios.put<AdminUserEntity>(
      `/api/admin/${userId}/unlock`,
    );
    return data;
  }

  async deleteUser(userId: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(`/api/admin/${userId}/delete`);
    return data;
  }
}
