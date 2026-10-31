import { QuestionReviewAction, QuestionReviewInput } from "@/domain/entities/pendingQuestion.entity";
import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class ReviewPendingQuestionUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(id: string, action: QuestionReviewAction, input: QuestionReviewInput, accessToken: string) {
    return this.repository.reviewPendingQuestion(id, action, input, accessToken);
  }
}
