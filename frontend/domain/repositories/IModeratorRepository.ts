import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

export interface IModeratorRepository {
  getQuestionFiles(accessToken: string): Promise<QuestionFileEntity[]>;
  deleteQuestionFile(key: string, accessToken: string): Promise<void>;
  parse(formData: FormData, accessToken: string): Promise<string[]>;
}
