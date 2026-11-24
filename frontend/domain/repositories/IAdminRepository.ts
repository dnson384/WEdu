import { AdminUserEntity } from "../entities/adminUser.entity";

export interface IAdminRepository {
  getAllUsers(accessToken: string): Promise<AdminUserEntity[]>;

  findUsers(keyword: string, accessToken: string): Promise<AdminUserEntity[]>;

  updateUserRole(
    userId: string,
    role: string,
    accessToken: string,
  ): Promise<AdminUserEntity>;

  lockUser(userId: string, accessToken: string): Promise<AdminUserEntity>;

  unlockUser(userId: string, accessToken: string): Promise<AdminUserEntity>;

  deleteUser(userId: string, accessToken: string): Promise<boolean>;
}
