export interface CreateDraftPayloadEntity {
  examName: string;
  questionsCount: number;
  questionTypes: string[];
}

export interface MatrixItems {
  questionType: string;
  difficultyLevel: string;
  selectedCount: number;
}

export interface MatrixDetailsItems {
  exerciseType: string;
  difficultyLevel: string;
  learningOutcome: string;
  questionType: string;
  selectedCount: number;
}

export interface LessonDraft {
  id: string;
  name: string;
  matrix: MatrixItems[];
  matrixDetails: MatrixDetailsItems[];
}

export interface ChapterDraft {
  id: string;
  name: string;
  lessons: LessonDraft[];
}

export interface DraftEntity {
  id: string;
  questionsCount: number;
  questionTypes: string[];
  chapters: ChapterDraft[];
  createAt?: Date;
  expiredAt?: Date;
}

export interface UpdateDraftParamEntity {
  id: string;
  name: string;
}

export interface UpdateChaptersDraftPayloadEntity {
  draftId: string;
  add: UpdateDraftParamEntity[];
  del: Array<string>;
}

export interface UpdateLessonsDraftPayloadEntity {
  draftId: string;
  chapterId: string;
  add: UpdateDraftParamEntity[];
  del: Array<string>;
}
