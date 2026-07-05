export interface LessonData {
  id: string;
  name: string;
  exerciseTypes: string[];
  difficultyLevels: string[];
  learningOutcomes: string[];
  questionTypes: string[];
}

export interface CategoryEntity {
  id: string;
  chapter: string;
  lessons: LessonData[];
}
