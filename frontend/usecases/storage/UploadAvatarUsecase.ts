import { IStorageRepository } from "@/domain/repositories/IStorageRepository";

export class UploadAvatarUsecase {
  constructor(private readonly repo: IStorageRepository) {}

  async execute(formData: FormData, accessToken: string): Promise<string> {
    return this.repo.uploadAvatar(formData, accessToken);
  }
}
