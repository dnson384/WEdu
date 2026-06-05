import { DraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class GetDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(draftId: string): Promise<DraftEntity> {
    return await this.repo.getDraft(draftId);
  }
}
