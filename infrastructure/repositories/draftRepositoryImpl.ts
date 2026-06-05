import axios from "axios";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";
import {
  CreateDraftPayloadEntity,
  DraftEntity,
  UpdateChaptersDraftPayloadEntity,
  UpdateLessonsDraftPayloadEntity,
} from "@/domain/entities/draft.entity";

export class DraftRepositoryImpl implements IDraftRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async createDraft(payload: CreateDraftPayloadEntity): Promise<string> {
    const { data } = await axios.post<string>(
      `${this.baseUrl}/draft/create`,
      payload,
    );
    return data;
  }

  async getDraft(draftId: string): Promise<DraftEntity> {
    const { data } = await axios.get<DraftEntity>(
      `${this.baseUrl}/draft/${draftId}`,
    );
    return data;
  }

  async updateChapters(
    payload: UpdateChaptersDraftPayloadEntity,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/chapter`,
      payload,
    );
    return data;
  }

  async updateLessons(
    payload: UpdateLessonsDraftPayloadEntity,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/lesson`,
      payload,
    );
    return data;
  }

  async generateMatrix(draftId: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix`,
      {},
      {
        params: {
          draftId: draftId,
        },
      },
    );
    return data;
  }

  async generateMatrixDetails(draftId: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix-details`,
      {},
      {
        params: {
          draftId: draftId,
        },
      },
    );
    return data;
  }
}
