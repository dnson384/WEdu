import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class GetMeUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(accessToken: string) {
    return await this.repo.getMe(accessToken);
  }
}
