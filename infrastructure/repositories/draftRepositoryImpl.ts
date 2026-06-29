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

  async createDraft(
    payload: CreateDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<string> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.post<string>(
      `${this.baseUrl}/draft/create`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async getDraft(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<DraftEntity>(
      `${this.baseUrl}/draft/${draftId}`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async updateChapters(
    payload: UpdateChaptersDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/chapter`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async updateLessons(
    payload: UpdateLessonsDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/lesson`,
      payload,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async generateMatrix(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix`,
      {},
      {
        params: {
          draftId: draftId,
        },

        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async generateMatrixDetails(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.put<boolean>(
      `${this.baseUrl}/draft/generate-matrix-details`,
      {},
      {
        params: {
          draftId: draftId,
        },
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );
    return data;
  }

  async getRecentDraft(
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity[]> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/recent`,
      {
        headers: {
          Authorization: `Bearer ${accessToken}`,
          Cookie: customCookieHeader,
        },
      },
    );

    return data;
  }

  async getAllUserDrafts(
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity[]> {
    const cookieHeaderParts: string[] = [];
    cookieHeaderParts.push(`accessToken=${accessToken}`);
    cookieHeaderParts.push(`refreshToken=${refreshToken}`);

    const customCookieHeader = cookieHeaderParts.join("; ");

    const { data } = await axios.get<DraftEntity[]>(
      `${this.baseUrl}/draft/all`,
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
