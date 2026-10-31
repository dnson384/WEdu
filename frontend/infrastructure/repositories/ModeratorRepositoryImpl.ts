import axios from "axios";
import {
  PendingQuestionFilters,
  PendingQuestionPage,
  QuestionReviewAction,
  QuestionReviewInput,
  QuestionReviewResult,
} from "@/domain/entities/pendingQuestion.entity";
import { IModeratorRepository } from "@/domain/repositories/IModeratorRepository";
import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import {
  ApproveQuestionFileResult,
  RejectQuestionFileInput,
} from "@/domain/entities/questionFileReview.entity";

export class ModeratorRepositoryImpl implements IModeratorRepository {
  async reviewPendingQuestion(id: string, action: QuestionReviewAction, input: QuestionReviewInput, accessToken: string): Promise<QuestionReviewResult | void> {
    const { data } = await axios.post<QuestionReviewResult | void>(`${this.baseUrl}/moderator/questions/${id}/${action}`, input, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    return data;
  }

  async editPendingQuestion(id: string, data: FormData, accessToken: string): Promise<void> {
    await axios.patch(`${this.baseUrl}/moderator/questions/${id}`, data, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }

  async getPendingQuestions(
    filters: PendingQuestionFilters,
    accessToken: string,
  ): Promise<PendingQuestionPage> {
    const { data } = await axios.get<PendingQuestionPage>(
      `${this.baseUrl}/moderator/questions/pending`,
      {
        params: filters,
        headers: { Authorization: `Bearer ${accessToken}` },
      },
    );
    return data;
  }

  async approveQuestionFile(
    formData: FormData,
    accessToken: string,
  ): Promise<ApproveQuestionFileResult> {
    const { data } = await axios.post<ApproveQuestionFileResult>(
      `${this.baseUrl}/moderator/question-files/approve`,
      formData,
      { headers: { Authorization: `Bearer ${accessToken}` } },
    );
    return data;
  }

  async rejectQuestionFile(
    data: RejectQuestionFileInput,
    accessToken: string,
  ): Promise<void> {
    await axios.post(`${this.baseUrl}/moderator/question-files/reject`, data, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
  }
  async getQuestionFileContent(
    key: string,
    accessToken: string,
  ): Promise<ArrayBuffer> {
    const { data } = await axios.get<ArrayBuffer>(
      `${this.baseUrl}/moderator/question-files/content`,
      {
        params: { key },
        responseType: "arraybuffer",
        headers: { Authorization: `Bearer ${accessToken}` },
      },
    );
    return data;
  }

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
