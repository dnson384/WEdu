import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class DeleteUserUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(userId: string, accessToken: string): Promise<boolean> {
    return this.repo.deleteUser(userId, accessToken);
  }
}
