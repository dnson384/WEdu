import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GenerateMatrixUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(draftId: string, accessToken: string, refreshToken: string) {
    return await this.draftRepository.generateMatrix(
      draftId,
      accessToken,
      refreshToken,
    );
  }
}
