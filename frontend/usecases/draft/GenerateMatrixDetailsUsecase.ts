import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GenerateMatrixDetailsUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(draftId: string, accessToken: string) {
    return await this.repo.generateMatrixDetails(draftId, accessToken);
  }
}
