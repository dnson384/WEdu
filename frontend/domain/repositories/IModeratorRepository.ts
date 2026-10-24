import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";

export interface IModeratorRepository {
  getQuestionFiles(accessToken: string): Promise<QuestionFileEntity[]>;
  parse(formData: FormData, accessToken: string): Promise<string[]>;
}
