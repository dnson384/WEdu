import { GeneratePracticePayload } from "../entities/generatePractice.entity";

export interface IQuestionRepository {
  generatePractice(payload: GeneratePracticePayload): Promise<any>;
}
