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
    refreshToken: string,
  ): Promise<ExamGeneratedResponseEntity> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.post<ExamGeneratedResponseEntity>(
      `${this.baseUrl}/exam/generate`,
      { draftId },
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async getExamById(
    examId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamDetailReponseEntity> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<ExamDetailReponseEntity>(
      `${this.baseUrl}/exam/${examId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  async getAllExams(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/all`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async exportExamWordFile(
    payload: ExportEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<Buffer> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/exporter/word`,
      payload,
      {
        responseType: "arraybuffer",

        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  async getRecentExam(
    accessToken: string,
    refreshToken: string,
  ): Promise<ExamResponseEntity[]> {
    const cookieHeaderParts: string[] = [];

    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<ExamResponseEntity[]>(
      `${this.baseUrl}/exam/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }
}
