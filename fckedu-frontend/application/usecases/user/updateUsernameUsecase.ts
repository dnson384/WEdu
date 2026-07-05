import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class UpdateUsernameUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(
    username: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    return this.repo.updateUsername(username, accessToken, refreshToken);
  }
}
