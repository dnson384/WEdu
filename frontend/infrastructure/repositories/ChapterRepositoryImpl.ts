import axios from "axios";
import { IChapterRepository } from "@/domain/repositories/IChapterRepository";
import { ChapterEntity } from "@/domain/entities/chapter.entity";

export class ChapterRepositoryImpl implements IChapterRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async getAll(accessToken: string): Promise<ChapterEntity[]> {
    const { data } = await axios.get<ChapterEntity[]>(
      `${this.baseUrl}/chapter/chapters`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }
}
