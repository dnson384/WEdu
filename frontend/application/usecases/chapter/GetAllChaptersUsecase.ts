import { ChapterEntity } from "@/domain/entities/chapter.entity";
import { IChapterRepository } from "@/domain/repositories/IChapterRepository";

export class GetAllChaptersUsecase {
  constructor(private readonly repo: IChapterRepository) {}

  async execute(
    accessToken: string,
    refreshToken: string,
  ): Promise<ChapterEntity[]> {
    return await this.repo.getAll(accessToken, refreshToken);
  }
}
