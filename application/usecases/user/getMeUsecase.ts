import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class getMeUsecase {
  constructor(private readonly userRepo: IUserRepository) {}

  async execute() {
    return await this.userRepo.getMe();
  }
}
