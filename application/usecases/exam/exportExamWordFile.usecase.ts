import { IExamsRepository } from "@/domain/repositories/IExamRepository";
import { ExamExportPayloadEntity } from '@/domain/entities/exam.entity';

export class ExportExamWordFileUsecase {
  constructor(private readonly repo: IExamsRepository) {}

  async execute(payload: ExamExportPayloadEntity[]): Promise<Buffer> {
    return await this.repo.exportExamWordFile(payload);
  }
}
