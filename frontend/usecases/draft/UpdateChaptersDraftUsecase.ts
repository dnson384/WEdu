import { UpdateDraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class UpdateChaptersDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(
    draftId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ) {
    return await this.repo.updateChapters(draftId, payload, accessToken);
  }
}
