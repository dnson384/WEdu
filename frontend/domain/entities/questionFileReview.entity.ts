export interface ApproveQuestionFileResult {
  questionIds: string[];
  fileDeleted: boolean;
  message: string;
}

export interface RejectQuestionFileInput {
  key: string;
  reason: string;
}
