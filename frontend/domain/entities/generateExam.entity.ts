export interface LessonPayload {
  name: string;
  questionsCount: number;
  exerciseTypes: string[];
  difficultyLevels: string[];
  learningOutcomes: string[];
  questionTypes: string[];
}

export interface GenerateExamPayload {
  title: string;
  chapter: string;
  lessons: LessonPayload[];
}
