import { QuestionEntity } from "@/domain/entities/question.entity";

export interface PendingQuestionEntity extends QuestionEntity {
  chapterId: string;
  lessonId: string;
  status: string;
  createdAt: string | null;
  updatedAt: string | null;
  regenerationCount: number;
  reviewRevision: number;
  latestRegeneratedQuestion: Pick<QuestionEntity, "question" | "options"> | null;
}

export type QuestionReviewAction = "approve" | "reject";

export interface QuestionReviewInput {
  updatedAt: string | null;
  reviewRevision: number;
}

export interface QuestionReviewResult {
  status: "PENDING_REVIEW" | "NEEDS_CONSIDERATION";
  regenerationCount: number;
}

export interface PendingQuestionPage {
  items: PendingQuestionEntity[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface PendingQuestionFilters {
  page: number;
  search: string;
  questionType: string;
}
