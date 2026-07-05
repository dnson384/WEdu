import { ChangePasswordPayloadEntity } from "@/domain/entities/user.entity";
import { IUserRepository } from "@/domain/repositories/IUserRepository";

export class changePasswordUsecase {
  constructor(private readonly repo: IUserRepository) {}

  async execute(
    paylaod: ChangePasswordPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    return this.repo.changePassword(paylaod, accessToken, refreshToken);
  }
}
