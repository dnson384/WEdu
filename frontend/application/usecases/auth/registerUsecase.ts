import { RegisterPayloadEntity } from "@/domain/entities/auth.entity";
import { IAuthRepository } from "@/domain/repositories/IAuthRepository";

export default class RegisterUsecase {
  constructor(private readonly authRepo: IAuthRepository) {}

  async execute(payload: RegisterPayloadEntity) {
    return await this.authRepo.register(payload);
  }
}
