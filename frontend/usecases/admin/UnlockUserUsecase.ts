import { UserEntity } from "@/domain/entities/user.entity";
import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class UnlockUserUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(userId: string, accessToken: string): Promise<UserEntity> {
    return this.repo.unlockUser(userId, accessToken);
  }
}
