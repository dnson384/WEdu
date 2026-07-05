import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export default class LogoutUsecase {
  constructor(private readonly authRepo: IAuthRepository) {}

  async execute() {
    return await this.authRepo.logout();
  }
}
