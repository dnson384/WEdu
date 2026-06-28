import { UserRepositoryImpl } from "@/infrastructure/repositories/userRepositoryImpl";

export class UpdateUsernameUsecase {
  constructor(private readonly repo: UserRepositoryImpl) {}

  async execute(username: string): Promise<boolean> {
    return this.repo.updateUsername(username);
  }
}
