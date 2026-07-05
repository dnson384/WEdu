import { ExamResponseEntity } from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GetRecentExamUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]> {
    return await this.repo.getRecentExam(accessToken, refreshToken);
  }
}
