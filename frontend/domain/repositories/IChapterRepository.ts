import { ChapterEntity } from "../entities/chapter.entity";

export interface IChapterRepository {
  getAll(accessToken: string): Promise<ChapterEntity[]>;
}
