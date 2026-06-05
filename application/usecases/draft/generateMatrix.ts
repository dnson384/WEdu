import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GenerateMatrixUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(draftId: string) {
    return await this.draftRepository.generateMatrix(draftId);
  }
}
