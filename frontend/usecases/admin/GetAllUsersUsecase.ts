import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class GetAllUsersUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(accessToken: string) {
    return await this.repo.getAllUsers(accessToken);
  }
}
