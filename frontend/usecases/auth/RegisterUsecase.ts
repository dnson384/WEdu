import { NewUserRequestEntity } from "@/domain/entities/auth.entity";
import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export default class RegisterUsecase {
  constructor(private readonly authRepo: IAuthRepository) {}

  async execute(payload: NewUserRequestEntity) {
    return await this.authRepo.register(payload);
  }
}
