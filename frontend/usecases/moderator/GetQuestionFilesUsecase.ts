import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class GetQuestionFilesUsecase {
  constructor(private readonly repo: IModeratorRepository) {}

  execute(accessToken: string) {
    return this.repo.getQuestionFiles(accessToken);
  }
}
