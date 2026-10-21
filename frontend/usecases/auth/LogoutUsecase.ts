import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export default class LogoutUsecase {
  constructor(private readonly repo: IAuthRepository) {}

  async execute(accessToken: string): Promise<boolean> {
    return await this.repo.logout(accessToken);
  }
}
