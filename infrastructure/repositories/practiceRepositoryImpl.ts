import axios from "axios";
import { IPracticeRepository } from "@/domain/repositories/IPracticeRepository";
import {
  ExportPayload,
  PracticeDetailEntity,
  PracticeEntity,
} from "@/domain/entities/practice.entity";

export class PracticeRepositoryImpl implements IPracticeRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async getAllPractices(): Promise<PracticeEntity[]> {
    const { data } = await axios.get<PracticeEntity[]>(
      `${this.baseUrl}/practice/all`,
    );
    return data;
  }

  async getPracticeById(id: string): Promise<PracticeDetailEntity> {
    const { data } = await axios.get<PracticeDetailEntity>(
      `${this.baseUrl}/practice/${id}`,
    );
    return data;
  }

  async exportWordFile(payload: ExportPayload): Promise<Buffer> {
    const { data } = await axios.post<Buffer>(
      `${this.baseUrl}/export/word`,
      payload,
      {
        responseType: "arraybuffer",
      },
    );
    return data;
  }
}
