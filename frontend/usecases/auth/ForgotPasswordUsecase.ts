import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export class ForgotPasswordUsecase {
  constructor(private readonly repo: IAuthRepository) {}

  async execute(email: string): Promise<string> {
    return await this.repo.forgotPassword(email);
  }
}
