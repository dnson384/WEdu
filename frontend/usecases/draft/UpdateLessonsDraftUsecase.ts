import { UpdateDraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class UpdateLessonsDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(
    draftId: string,
    chapterId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ) {
    return await this.repo.updateLessons(
      draftId,
      chapterId,
      payload,
      accessToken,
    );
  }
}
