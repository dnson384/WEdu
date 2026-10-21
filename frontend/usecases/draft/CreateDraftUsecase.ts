import { CreateDraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export default class CreateDraftUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(payload: CreateDraftEntity, accessToken: string) {
    return await this.repo.createDraft(payload, accessToken);
  }
}
