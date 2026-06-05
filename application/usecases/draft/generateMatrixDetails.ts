import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GenerateMatrixDetailsUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(draftId: string) {
    return await this.draftRepository.generateMatrixDetails(draftId);
  }
}
