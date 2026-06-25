import { ExamResponseEntity } from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GetRecentExamUsecase {
  constructor(private readonly repo: IExamsRepository){}

  async execute(): Promise<ExamResponseEntity[]> {
    return await this.repo.getRecentExam();
  }
}