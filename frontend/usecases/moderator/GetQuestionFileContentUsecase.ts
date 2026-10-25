import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class GetQuestionFileContentUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(key: string, accessToken: string): Promise<ArrayBuffer> {
    return this.repository.getQuestionFileContent(key, accessToken);
  }
}
