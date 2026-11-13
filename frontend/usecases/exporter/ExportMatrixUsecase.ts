import { IExporterRepository } from "@/domain/repositories/IExporterRepository";

export class ExportMatrixUsecase {
  constructor(private readonly repo: IExporterRepository) {}

  execute(examId: string, examName: string, accessToken: string): Promise<Buffer> {
    return this.repo.exportMatrix(examId, examName, accessToken);
  }
}
