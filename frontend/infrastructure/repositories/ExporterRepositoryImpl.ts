import { IExporterRepository } from "@/domain/repositories/IExporterRepository";
import axios from "axios";

export class ExporterRepositoryImpl implements IExporterRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async exportExam(
    examId: string,
    examName: string,
    accessToken: string,
  ): Promise<Buffer> {
    return this.exportFile("exam", examId, examName, accessToken);
  }

  async exportMatrix(examId: string, examName: string, accessToken: string): Promise<Buffer> {
    return this.exportFile("matrix", examId, examName, accessToken);
  }

  private async exportFile(
    kind: "exam" | "matrix", examId: string, examName: string, accessToken: string,
  ): Promise<Buffer> {
    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exporter/${kind}`,
      { examId, examName },
      {
        responseType: "arraybuffer",
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }
}
