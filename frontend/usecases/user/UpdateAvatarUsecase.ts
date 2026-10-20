import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class UpdateAvatarUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(s3Key: string, accessToken: string): Promise<boolean> {
    return this.repo.updateAvatar(s3Key, accessToken);
  }
}
