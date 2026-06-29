import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class UploadAvatarUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(
    formData: FormData,
    accessToken: string,
    refreshToken: string,
  ): Promise<string> {
    return this.repo.uploadAvatar(formData, accessToken, refreshToken);
  }
}
