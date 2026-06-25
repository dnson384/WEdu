import {
  ExamExportPayloadEntity,
  ExamDetailReponseEntity,
  ExamResponseEntity,
} from "../entities/exam.entity";

export interface IExamsRepository {
  generateExam(draftId: string): Promise<boolean>;
  getExamById(examId: string): Promise<ExamDetailReponseEntity>;
  getAllExams(): Promise<ExamResponseEntity[]>;
  exportExamWordFile(payload: ExamExportPayloadEntity[]): Promise<Buffer>;
  getRecentExam(): Promise<ExamResponseEntity[]>;
}
