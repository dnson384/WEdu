import { QuestionFileEntity } from "@/domain/entities/questionFile.entity";
import {
  PendingQuestionFilters,
  PendingQuestionPage,
  QuestionReviewAction,
  QuestionReviewInput,
  QuestionReviewResult,
} from "@/domain/entities/pendingQuestion.entity";
import {
  ApproveQuestionFileResult,
  RejectQuestionFileInput,
} from "@/domain/entities/questionFileReview.entity";

export interface IModeratorRepository {
  reviewPendingQuestion(id: string, action: QuestionReviewAction, input: QuestionReviewInput, accessToken: string): Promise<QuestionReviewResult | void>;
  editPendingQuestion(id: string, data: FormData, accessToken: string): Promise<void>;
  getPendingQuestions(
    filters: PendingQuestionFilters,
    accessToken: string,
  ): Promise<PendingQuestionPage>;
  approveQuestionFile(
    data: FormData,
    accessToken: string,
  ): Promise<ApproveQuestionFileResult>;
  rejectQuestionFile(
    data: RejectQuestionFileInput,
    accessToken: string,
  ): Promise<void>;
  getQuestionFileContent(
    key: string,
    accessToken: string,
  ): Promise<ArrayBuffer>;
  getQuestionFiles(accessToken: string): Promise<QuestionFileEntity[]>;
  deleteQuestionFile(key: string, accessToken: string): Promise<void>;
  parse(formData: FormData, accessToken: string): Promise<string[]>;
}
