import { IExamsRepository } from "@/domain/repositories/IExamRepository";

export class DeleteExamUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    examId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    return await this.repo.deleteExam(
      examId,
      accessToken,
      refreshToken,
    );
  }
}
