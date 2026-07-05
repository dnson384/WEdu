import {
  ExportEntity,
  ExamDetailReponseEntity,
  ExamResponseEntity,
} from "../entities/exam.entity";

export interface IExamsRepository {
  generateExam(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
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
  getRecentExam(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]>;
}
