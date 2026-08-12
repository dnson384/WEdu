export interface LessonData {
  id: string;
  name: string;
  exerciseTypes: string[];
  difficultyLevels: string[];
  learningOutcomes: string[];
  questionTypes: string[];
}

export interface ChapterEntity {
  id: string;
  name: string;
  lessons: LessonData[];
}
