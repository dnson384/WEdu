import axios from "axios";
import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

export class ModeratorRepositoryImpl implements IModeratorRepository {
  private readonly baseUrl =
    process.env.NODE_ENV === "development"
      ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
      : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;

  async getQuestionFiles(accessToken: string): Promise<QuestionFileEntity[]> {
    const { data } = await axios.get<QuestionFileEntity[]>(
      `${this.baseUrl}/moderator/question-files`,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    return data;
  }

  async deleteQuestionFile(key: string, accessToken: string): Promise<void> {
    await axios.delete<void>(`${this.baseUrl}/moderator/question-files`, {
      params: { key },
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  async parse(formData: FormData, accessToken: string): Promise<string[]> {
    const { data } = await axios.post<string[]>(
      `${this.baseUrl}/moderator/parse`,
      formData,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    return data;
  }
}
