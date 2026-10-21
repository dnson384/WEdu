export interface CreateDraftEntity {
  examName: string;
  questionsCount: number;
  questionTypes: string[];
}

// Response Entity
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
  examName: string;
  questionsCount: number;
  questionTypes: string[];
  chapters: ChapterDraft[];
}

// Update
export interface UpdateDraftParamEntity {
  id: string;
  name: string;
}

export interface UpdateDraftEntity {
  add: UpdateDraftParamEntity[];
  del: Array<string>;
}
