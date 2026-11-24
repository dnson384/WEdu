import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";
import { RejectQuestionFileInput } from "@/domain/entities/questionFileReview.entity";

export class RejectQuestionFileUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(data: RejectQuestionFileInput, accessToken: string) {
    return this.repository.rejectQuestionFile(data, accessToken);
  }
}
