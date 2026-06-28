import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";

export class UploadAvatarUsecase {
  constructor(private readonly repo: UserRepositoryImpl) {}

  async execute(formData: FormData): Promise<string> {
    return this.repo.uploadAvatar(formData);
  }
}
