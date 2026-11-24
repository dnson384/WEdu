import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class EditPendingQuestionUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(id: string, data: FormData, accessToken: string) {
    return this.repository.editPendingQuestion(id, data, accessToken);
  }
}
