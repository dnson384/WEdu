import { IPracticeRepository } from "@/domain/repositories/IPracticeRepository";
import { ExportPayload } from "@/domain/entities/practice.entity";

export class ExportWordFileUsecase {
  constructor(private readonly practiceRepository: IPracticeRepository) {}

  async execute(payload: ExportPayload): Promise<any> {
    return await this.practiceRepository.exportWordFile(payload);
  }
}
