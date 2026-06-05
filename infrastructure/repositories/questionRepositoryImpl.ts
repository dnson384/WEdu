import axios from "axios";
import { IQuestionRepository } from "@/domain/repositories/IQuestionRepository";
import { GeneratePracticePayload } from "@/domain/entities/generatePractice.entity";

export class QuestionRepositoryImpl implements IQuestionRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async generatePractice(payload: GeneratePracticePayload): Promise<any> {
    const { data } = await axios.post<string>(
      `${this.baseUrl}/question/generate-practice`,
      payload,
    );
    return data;
  }
}
