import { GeneratePracticePayload } from "@/domain/entities/generatePractice.entity";
import { IQuestionRepository } from "@/domain/repositories/IQuestionRepository";

export class GeneratePracticeUsecase {
  constructor(private readonly questionRepository: IQuestionRepository) {}

  async execute(payload: GeneratePracticePayload): Promise<any> {
    return await this.questionRepository.generatePractice(payload);
  }
}
