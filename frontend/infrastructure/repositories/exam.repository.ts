import {
  ExamDetailReponseEntity,
  ExamGeneratedResponseEntity,
  ExamResponseEntity,
  ExportEntity,
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

  async generateExam(
    draftId: string,
    accessToken: string,
  ): Promise<ExamGeneratedResponseEntity> {
    const { data } = await axios.post<ExamGeneratedResponseEntity>(
      `${this.baseUrl}/exam/generate`,
      { draftId },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async getExamById(
    examId: string,
    accessToken: string,
  ): Promise<ExamDetailReponseEntity> {
    const { data } = await axios.get<ExamDetailReponseEntity>(
      `${this.baseUrl}/exam/${examId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async getAllExams(accessToken: string): Promise<ExamResponseEntity[]> {
    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/all`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async exportExamWordFile(
    payload: ExportEntity,
    accessToken: string,
  ): Promise<Buffer> {
    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exporter/exam`,
      payload,
      {
        responseType: "arraybuffer",

        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async getRecentExam(accessToken: string): Promise<ExamResponseEntity[]> {
    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async deleteExam(examId: string, accessToken: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(
      `${this.baseUrl}/exam/delete/${examId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }
}
