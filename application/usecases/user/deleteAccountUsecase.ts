import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class deleteAccountUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(accessToken: string, refreshToken: string): Promise<boolean> {
    return this.repo.deleteAccount(accessToken, refreshToken);
  }
}
