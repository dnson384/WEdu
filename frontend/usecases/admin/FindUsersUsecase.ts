import { IAdminRepository } from "@/domain/repositories/IAdminRepository";

export class FindUsersUsecase {
  constructor(private readonly repo: IAdminRepository) {}

  async execute(keyword: string, accessToken: string) {
    return await this.repo.findUsers(keyword, accessToken);
  }
}
