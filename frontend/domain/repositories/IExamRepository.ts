import {
  ExamDetailReponseEntity,
  ExamResponseEntity,
  ExamGeneratedResponseEntity,
} from "../entities/exam.entity";

export interface IExamsRepository {
  generateExam(
    draftId: string,
    accessToken: string,
  ): Promise<ExamGeneratedResponseEntity>;

  getExamById(
    examId: string,
    accessToken: string,
  ): Promise<ExamDetailReponseEntity>;

  getAllExams(accessToken: string): Promise<ExamResponseEntity[]>;

  getRecentExam(accessToken: string): Promise<ExamResponseEntity[]>;

  deleteExam(examId: string, accessToken: string): Promise<boolean>;
}
