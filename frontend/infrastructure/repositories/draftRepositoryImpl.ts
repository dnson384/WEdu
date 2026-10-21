import axios from "axios";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";
import {
  CreateDraftEntity,
  DraftEntity,
  UpdateDraftEntity,
} from "@/domain/entities/draft.entity";

export class DraftRepositoryImpl implements IDraftRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async createDraft(
    payload: CreateDraftEntity,
    accessToken: string,
  ): Promise<string> {
    const { data } = await axios.post<string>(
      `${this.baseUrl}/draft/create`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async getDraft(draftId: string, accessToken: string): Promise<DraftEntity> {
    const { data } = await axios.get<DraftEntity>(
      `${this.baseUrl}/draft/${draftId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async updateChapters(
    draftId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/${draftId}/chapter`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async updateLessons(
    draftId: string,
    chapterId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/${draftId}/${chapterId}/lesson`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async generateMatrix(draftId: string, accessToken: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/${draftId}/generate-matrix`,
      {},
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async generateMatrixDetails(
    draftId: string,
    accessToken: string,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/${draftId}/generate-matrix-details`,
      {},
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );
    return data;
  }

  async getRecentDraft(accessToken: string): Promise<DraftEntity[]> {
    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async getAllUserDrafts(accessToken: string): Promise<DraftEntity[]> {
    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/user-drafts`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }

  async deleteDraft(draftId: string, accessToken: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(
      `${this.baseUrl}/draft/${draftId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
      },
    );

    return data;
  }
}
