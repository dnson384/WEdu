import axios from "axios";
import { ChapterEntity } from "@/domain/entities/chapter.entity";

export async function getAllChaptersService(): Promise<ChapterEntity[]> {
  const response = await axios.get<ChapterEntity[]>("/api/chapter/all");
  return response.data;
}
