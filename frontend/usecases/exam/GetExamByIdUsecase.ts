import { ExamDetailReponseEntity } from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GetExamByIdUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    examId: string,
    accessToken: string,
  ): Promise<ExamDetailReponseEntity> {
    return await this.repo.getExamById(examId, accessToken);
  }
}
