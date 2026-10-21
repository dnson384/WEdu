export interface IExporterRepository {
  exportExam(
    examId: string,
    examName: string,
    accessToken: string,
  ): Promise<Buffer>;
}
