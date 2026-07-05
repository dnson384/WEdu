import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class lockAccountUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(accessToken: string, refreshToken: string): Promise<boolean> {
    return this.repo.lockAccount(accessToken, refreshToken);
  }
}
