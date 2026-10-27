import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";
import { PendingQuestionFilters } from "@/domain/entities/pendingQuestion.entity";

export class GetPendingQuestionsUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(filters: PendingQuestionFilters, accessToken: string) {
    return this.repository.getPendingQuestions(filters, accessToken);
  }
}
