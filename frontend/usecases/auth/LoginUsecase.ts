import { LoginPayloadEntity } from "@/domain/entities/auth.entity";
import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export default class LoginUsecase {
  constructor(private readonly authRepo: IAuthRepository) {}

  async execute(payload: LoginPayloadEntity) {
    return await this.authRepo.login(payload);
  }
}
