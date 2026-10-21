import { DraftEntity } from "@/domain/entities/draft.entity";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";

export class GetAllUserDraftsUsecase {
  constructor(private readonly repo: IDraftRepository) {}

  async execute(accessToken: string): Promise<DraftEntity[]> {
    return this.repo.getAllUserDrafts(accessToken);
  }
}
