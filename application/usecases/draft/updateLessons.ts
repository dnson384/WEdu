import { UpdateLessonsDraftPayloadEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class UpdateLessonsDraftUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(payload: UpdateLessonsDraftPayloadEntity) {
    return await this.draftRepository.updateLessons(payload);
  }
}
