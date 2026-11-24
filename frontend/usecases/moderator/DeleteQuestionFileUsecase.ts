import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class DeleteQuestionFileUsecase {
  constructor(private readonly repo: IModeratorRepository) {}

  execute(key: string, accessToken: string): Promise<void> {
    return this.repo.deleteQuestionFile(key, accessToken);
  }
}
