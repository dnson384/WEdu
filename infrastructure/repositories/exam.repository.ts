import {
  ExamExportPayloadEntity,
  ExamDetailReponseEntity,
  ExamResponseEntity,
} from "@/domain/entities/exam.entity";
import { IExamsRepository } from "@/domain/repositories/IExamRepository";
import axios from "axios";
import { cookies } from "next/headers";

export class ExamsRepositoryImpl implements IExamsRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async generateExam(draftId: string): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.post<boolean>(
      `${this.baseUrl}/exam/generate`,
      { draftId },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async getExamById(examId: string): Promise<ExamDetailReponseEntity> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<ExamDetailReponseEntity>(
      `${this.baseUrl}/exam/${examId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );

    return data;
  }

  async getAllExams(): Promise<ExamResponseEntity[]> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/all`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async exportExamWordFile(
    payload: ExamExportPayloadEntity[],
  ): Promise<Buffer> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exam/export`,
      payload,
      {
        responseType: "arraybuffer",

        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );

    return data;
  }

  async getRecentExam(): Promise<ExamResponseEntity[]> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );

    return data;
  }
}
