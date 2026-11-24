export interface QuestionFileEntity {
  key: string;
  fileName: string;
  size: number;
  lastModified: string;
  presignedUrl: string;
}
