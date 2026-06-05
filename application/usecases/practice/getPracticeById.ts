import { IPracticeRepository } from "@/domain/repositories/IPracticeRepository";
import { PracticeDetailEntity } from "@/domain/entities/practice.entity";

export class GetPracticeByIdUsecase {
  constructor(private readonly practiceRepository: IPracticeRepository) {}

  async execute(id: string): Promise<PracticeDetailEntity> {
    return await this.practiceRepository.getPracticeById(id);
  }
}
