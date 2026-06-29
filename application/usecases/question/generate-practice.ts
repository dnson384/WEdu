import { IQuestionRepository } from "@/domain/repositories/IQuestionRepository";

export class GeneratePracticeUsecase {
  constructor(private readonly questionRepository: IQuestionRepository) {}
}
