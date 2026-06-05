import { ExamResponseEntity } from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class GetAllExamsUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(): Promise<ExamResponseEntity[]> {
    return await this.repo.getAllExams();
  }
}
