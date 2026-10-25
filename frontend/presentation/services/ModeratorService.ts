import axios from "axios";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import { ApproveQuestionFileResult, RejectQuestionFileInput } from "@/domain/entities/questionFileReview.entity";

export class ModeratorService {
  async approveQuestionFile(key: string, file: File | null): Promise<ApproveQuestionFileResult> {
    const form = new FormData();
    form.set("key", key);
    if (file) form.set("file", file);
    const { data } = await axios.post<ApproveQuestionFileResult>("/api/moderator/question-files/approve", form);
    return data;
  }

  async rejectQuestionFile(data: RejectQuestionFileInput): Promise<void> {
    await axios.post("/api/moderator/question-files/reject", data);
  }
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
