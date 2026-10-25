import axios from "axios";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

export class ModeratorService {
  async getQuestionFileContent(key: string, signal: AbortSignal): Promise<ArrayBuffer> {
    const { data } = await axios.get<ArrayBuffer>("/api/moderator/question-files/content", {
      params: { key }, responseType: "arraybuffer", signal,
    });
    return data;
  }
  async deleteQuestionFile(key: string): Promise<void> {
    await axios.delete<void>("/api/moderator/question-files", {
      params: { key },
    });
  }

  async getQuestionFiles(): Promise<QuestionFileEntity[]> {
    const { data } = await axios.get<QuestionFileEntity[]>(
      "/api/moderator/question-files",
    );
    return data;
  }
}
