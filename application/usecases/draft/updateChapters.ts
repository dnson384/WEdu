import { UpdateChaptersDraftPayloadEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class UpdateChaptersUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(payload: UpdateChaptersDraftPayloadEntity) {
    return await this.draftRepository.updateChapters(payload);
  }
}
