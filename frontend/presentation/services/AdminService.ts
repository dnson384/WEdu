import { UserEntity } from "@/domain/entities/user.entity";
import axios from "axios";

export class AdminService {
  constructor() {}

  async getAllUsers(): Promise<UserEntity[]> {
    const { data } = await axios.get<UserEntity[]>(`/api/admin/users`);
    return data;
  }

  async findUsers(keyword: string): Promise<UserEntity[]> {
    const { data } = await axios.get<UserEntity[]>(`/api/admin/users/search`, {
      params: {
        keyword,
      },
    });
    return data;
  }

  async updateUserRole(userId: string, role: string): Promise<UserEntity> {
    const { data } = await axios.put<UserEntity>(
      `/api/admin/${userId}/role`,
      {},
      { params: { role } },
    );
    return data;
  }

  async lockUser(userId: string): Promise<UserEntity> {
    const { data } = await axios.put<UserEntity>(`/api/admin/${userId}/lock`);
    return data;
  }

  async unLockUser(userId: string): Promise<UserEntity> {
    const { data } = await axios.put<UserEntity>(`/api/admin/${userId}/unlock`);
    return data;
  }

  async deleteUser(userId: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(`/api/admin/${userId}/delete`);
    return data;
  }
}
