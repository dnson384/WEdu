import { ChangePasswordEntity } from "@/domain/entities/auth.entity";
import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export class ChangePasswordUsecase {
  constructor(private readonly repo: IAuthRepository) {}

  async execute(
    payload: ChangePasswordEntity,
    accessToken: string,
  ): Promise<boolean> {
    return this.repo.changePassword(payload, accessToken);
  }
}
