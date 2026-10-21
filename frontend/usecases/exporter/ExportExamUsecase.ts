import { IExporterRepository } from "@/domain/repositories/IExporterRepository";

export class ExportExamUsecase {
  constructor(private readonly repo: IExporterRepository) {}

  async execute(
    examId: string,
    examName: string,
    accessToken: string,
  ): Promise<Buffer> {
    return await this.repo.exportExam(examId, examName, accessToken);
  }
}
