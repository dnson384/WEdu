import {
  ExportEntity,
  ExamDetailReponseEntity,
  ExamResponseEntity,
  ExamGeneratedResponseEntity,
} from "../entities/exam.entity";

export interface IExamsRepository {
  generateExam(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamGeneratedResponseEntity>;
  getExamById(
    examId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamDetailReponseEntity>;
  getAllExams(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]>;
  exportExamWordFile(
    payload: ExportEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<Buffer>;
  deleteExam(
    examId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  getRecentExam(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]>;
}
