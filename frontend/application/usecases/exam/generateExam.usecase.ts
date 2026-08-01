import { ExamGeneratedResponseEntity } from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GenerateExamUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamGeneratedResponseEntity> {
    return await this.repo.generateExam(draftId, accessToken, refreshToken);
  }
}
