import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export class ResetPasswordUsecase {
  constructor(private readonly repo: IAuthRepository) {}

  async execute(
    plainToken: string,
    newPassword: string,
    confirmNewPassword: string,
  ): Promise<string> {
    return this.repo.resetPassword(plainToken, newPassword, confirmNewPassword);
  }
}
