export interface QuestionContent {
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export interface OptionsData {
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export interface QuestionEntity {
  id: string;
  chapter: string;
  lesson: string;
  exerciseType: string;
  difficultyLevel: string;
  learningOutcomes: string[];
  questionType: string;
  question: QuestionContent;
  options: OptionsData[];
}
