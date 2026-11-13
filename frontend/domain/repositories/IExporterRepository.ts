export interface IExporterRepository {
  exportMatrix(examId: string, examName: string, accessToken: string): Promise<Buffer>;
  exportExam(
    examId: string,
    examName: string,
    accessToken: string,
  ): Promise<Buffer>;
}
