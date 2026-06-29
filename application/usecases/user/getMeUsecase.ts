import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class getMeUsecase {
  constructor(private readonly userRepo: IUserRepository) {}

  async execute(accessToken: string, refreshToken: string) {
    return await this.userRepo.getMe(accessToken, refreshToken);
  }
}
