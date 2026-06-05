import { IPracticeRepository } from "@/domain/repositories/IPracticeRepository";
import { PracticeEntity } from "@/domain/entities/practice.entity";

export class GetAllPracticesUsecase {
  constructor(private readonly practiceRepository: IPracticeRepository) {}

  async execute(): Promise<PracticeEntity[]> {
    return await this.practiceRepository.getAllPractices();
  }
}
