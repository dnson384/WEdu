import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GenerateMatrixUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(draftId: string, accessToken: string) {
    return await this.repo.generateMatrix(draftId, accessToken);
  }
}
