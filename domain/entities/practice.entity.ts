import { QuestionEntity } from "./question.entity";

export interface PracticeEntity {
  id: string;
  title: string;
  chapter: string;
  questionsCount: number;
  questionsId: string[];
}

export interface PracticeDetailEntity {
  title: string;
  chapter: string;
  questionsCount: number;
  questions: QuestionEntity[];
}

// Export
export interface ContentExport {
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export interface QuestionExport {
  id: string;
  questionType: string;
  question: ContentExport;
  options: ContentExport[];
}

export interface LessonExport {
  [lessonName: string]: QuestionExport[];
}

export interface ExportPayload {
  title: string
  questionsSorted: LessonExport
}
