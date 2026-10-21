import axios from "axios";
import { ChapterEntity } from "@/domain/entities/chapter.entity";

export class ChapterSerivce {
  constructor() {}

  async getAll(): Promise<ChapterEntity[]> {
    const { data } = await axios.get<ChapterEntity[]>("/api/chapter");
    return data;
  }
}
