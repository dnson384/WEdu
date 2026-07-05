import { IExamsRepository } from "@/domain/repositories/IExamRepository";
import { ExportEntity } from "@/domain/entities/exam.entity";

export class ExportExamWordFileUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(
    payload: ExportEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<Buffer> {
    return await this.repo.exportExamWordFile(
      payload,
      accessToken,
      refreshToken,
    );
  }
}
