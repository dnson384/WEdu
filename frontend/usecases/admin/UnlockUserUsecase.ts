import { AdminUserEntity } from "@/domain/entities/adminUser.entity";
import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class UnlockUserUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(userId: string, accessToken: string): Promise<AdminUserEntity> {
    return this.repo.unlockUser(userId, accessToken);
  }
}
