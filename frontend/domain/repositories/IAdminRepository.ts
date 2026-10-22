import { UserEntity } from "../entities/user.entity";

export interface IAdminRepository {
  getAllUsers(accessToken: string): Promise<UserEntity[]>;

  findUsers(keyword: string, accessToken: string): Promise<UserEntity[]>;

  updateUserRole(
    userId: string,
    role: string,
    accessToken: string,
  ): Promise<UserEntity>;

  lockUser(userId: string, accessToken: string): Promise<UserEntity>;

  unlockUser(userId: string, accessToken: string): Promise<UserEntity>;

  deleteUser(userId: string, accessToken: string): Promise<boolean>;
}
