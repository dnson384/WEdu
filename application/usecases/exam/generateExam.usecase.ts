import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GenerateExamUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    return await this.repo.generateExam(draftId, accessToken, refreshToken);
  }
}
