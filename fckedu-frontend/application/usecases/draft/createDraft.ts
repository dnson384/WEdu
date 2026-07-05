import { CreateDraftPayloadEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class CreateDraftUsecase {
  constructor(private readonly draftRepository: IDraftRepository) {}

  async execute(
    payload: CreateDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ) {
    return await this.draftRepository.createDraft(
      payload,
      accessToken,
      refreshToken,
    );
  }
}
