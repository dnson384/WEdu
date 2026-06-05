export interface Content {
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export interface QuestionDetail {
  id: string;
  question: Content;
  options: Content[];
}

export interface ExamQuestion {
  questionType: string;
  difficultyLevel: string;
  questions: QuestionDetail[];
}

export interface ExamChapterReposneEntity {
  id: string
  lessonIds: string[]
}

export interface ExamResponseEntity {
  id: string
  name: string
  chapters: ExamChapterReposneEntity[]
  questionsCount: number
}

export interface ExamDetailReponseEntity {
  id: string;
  name: string;
  groups: ExamQuestion[];
}

export interface ExamExportPayloadEntity {
  questionType: string;
  questionIds: string[];
}
