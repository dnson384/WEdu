import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export class DeleteDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    return await this.repo.deleteDraft(
      draftId,
      accessToken,
      refreshToken,
    );
  }
}
