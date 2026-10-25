import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";

export class ApproveQuestionFileUsecase {
  constructor(private readonly repository: IModeratorRepository) {}

  execute(data: FormData, accessToken: string) {
    return this.repository.approveQuestionFile(data, accessToken);
  }
}
