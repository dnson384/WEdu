import { QuestionEntity } from "@/domain/entities/question.entity";

export interface PendingQuestionEntity extends QuestionEntity {
  chapterId: string;
  lessonId: string;
  status: string;
  createdAt: string | null;
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
