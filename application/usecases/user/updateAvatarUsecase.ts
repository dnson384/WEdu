import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";

export class UpdateAvatarUsecase {
  constructor(private readonly repo: UserRepositoryImpl) {}

  async execute(s3Key: string): Promise<boolean> {
    return this.repo.updateAvatar(s3Key);
  }
}
