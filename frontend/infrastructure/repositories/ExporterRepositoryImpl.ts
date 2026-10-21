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
    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exporter/exam`,
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
