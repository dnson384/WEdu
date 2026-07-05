import { DraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export class GetRecentDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity[]> {
    return this.repo.getRecentDraft(accessToken, refreshToken);
  }
}
