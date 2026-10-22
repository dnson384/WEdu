import { UserEntity } from "@/domain/entities/user.entity";
import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class UpdateUserRoleUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(
    userId: string,
    role: string,
    accessToken: string,
  ): Promise<UserEntity> {
    return this.repo.updateUserRole(userId, role, accessToken);
  }
}
