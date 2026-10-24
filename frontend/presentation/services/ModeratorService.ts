import axios from "axios";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

export class ModeratorService {
  async getQuestionFiles(): Promise<QuestionFileEntity[]> {
    const { data } = await axios.get<QuestionFileEntity[]>("/api/moderator/question-files");
    return data;
  }
}
