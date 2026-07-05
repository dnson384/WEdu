export interface ContentDTO {
  template: string;
  variables: {
    math: Record<string, string>;
    image: Record<string, string>;
  };
}

export interface QuestionDataDTO {
  id: string;
  question: ContentDTO;
  options: ContentDTO[];
}

interface QuestionSortedData {
  questionType: string;
  questionsData: QuestionDataDTO[];
}

export interface ImageCacheData {
  questions: Record<string, string>;
  options: Record<string, string>;
}

export interface WordPayloadRequestDTO {
  examName: string;
  imageCache: ImageCacheData;
  questionsSorted: QuestionSortedData[];
}
