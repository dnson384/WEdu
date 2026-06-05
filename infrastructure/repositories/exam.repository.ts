import {
  ExamExportPayloadEntity,
  ExamDetailReponseEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";
import axios from "axios";

export class ExamsRepositoryImpl implements IExamsRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async generateExam(draftId: string): Promise<boolean> {
    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/exam/generate`,
      { draftId },
    );
    return data;
  }

  async getExamById(examId: string): Promise<ExamDetailReponseEntity> {
    const { data } = await axios.get<ExamDetailReponseEntity>(
      `${this.baseUrl}/exam/${examId}`,
    );

    return data;
  }

  async getAllExams(): Promise<ExamResponseEntity[]> {
    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/all`,
    );
    return data;
  }

  async exportExamWordFile(
    payload: ExamExportPayloadEntity[],
  ): Promise<Buffer> {
    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exam/export`,
      payload,
      {
        responseType: "arraybuffer",
      },
    );

    return data;
  }
}
