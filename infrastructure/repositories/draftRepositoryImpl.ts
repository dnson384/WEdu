import axios from "axios";
import { IDraftRepository } from "@/domain/repositories/IDraftRepository";
import {
  CreateDraftPayloadEntity,
  DraftEntity,
  UpdateChaptersDraftPayloadEntity,
  UpdateLessonsDraftPayloadEntity,
} from "@/domain/entities/draft.entity";
import { cookies } from "next/headers";

export class DraftRepositoryImpl implements IDraftRepository {
  private readonly baseUrl: string;

  constructor() {
    this.baseUrl =
      process.env.NODE_ENV === "development"
        ? process.env.NEXT_PUBLIC_BACKEND_DEV_URL!
        : process.env.NEXT_PUBLIC_BACKEND_PROD_URL!;
  }

  async createDraft(payload: CreateDraftPayloadEntity): Promise<string> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.post<string>(
      `${this.baseUrl}/draft/create`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async getDraft(draftId: string): Promise<DraftEntity> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<DraftEntity>(
      `${this.baseUrl}/draft/${draftId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async updateChapters(
    payload: UpdateChaptersDraftPayloadEntity,
  ): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/chapter`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async updateLessons(
    payload: UpdateLessonsDraftPayloadEntity,
  ): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/lesson`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async generateMatrix(draftId: string): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix`,
      {},
      {
        params: {
          draftId: draftId,
        },

        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async generateMatrixDetails(draftId: string): Promise<boolean> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix-details`,
      {},
      {
        params: {
          draftId: draftId,
        },
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );
    return data;
  }

  async getRecentDraft(): Promise<DraftEntity[]> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
        },
        withCredentials: true,
      },
    );

    return data;
  }

  async getAllUserDrafts(): Promise<DraftEntity[]> {
    const cookieStore = await cookies();
    const accessToken = cookieStore.get("accessToken")?.value;

    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/all`,
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
