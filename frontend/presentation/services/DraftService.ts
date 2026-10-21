import axios from "axios";
import {
  CreateDraftPayload,
  UpdateDraftPayload,
} from "../schemas/draft.schema";
import { DraftEntity } from "@/domain/entities/draft.entity";

export class DraftService {
  constructor() {}

  async create(payload: CreateDraftPayload): Promise<string> {
    const { data } = await axios.post<string>(`/api/draft/create`, payload);
    return data;
  }

  async get(draftId: string): Promise<DraftEntity> {
    const { data } = await axios.get<DraftEntity>(`/api/draft/${draftId}`);
    return data;
  }

  async updateChapters(
    draftId: string,
    payload: UpdateDraftPayload,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `/api/draft/${draftId}/chapter`,
      payload,
    );
    return data;
  }

  async updateLessons(
    draftId: string,
    chapterId: string,
    payload: UpdateDraftPayload,
  ): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `/api/draft/${draftId}/${chapterId}/lesson`,
      payload,
    );
    return data;
  }

  async generateMatrix(draftId: string): Promise<boolean> {
    const { data } = await axios.put<boolean>(
      `/api/draft/${draftId}/generate-matrix`,
    );
    return data;
  }

  async generateMatrixDetails(draftId: string): Promise<DraftEntity> {
    const { data } = await axios.put<DraftEntity>(
      `/api/draft/${draftId}/generate-matrix-details`,
    );
    return data;
  }

  async getRecent(): Promise<DraftEntity[]> {
    const { data } = await axios.get<DraftEntity[]>(`/api/draft/recent`);
    return data;
  }

  async getAll(): Promise<DraftEntity[]> {
    const { data } = await axios.get<DraftEntity[]>(`/api/draft/user-drafts`);
    return data;
  }

  async delete(draftId: string): Promise<boolean> {
    const { data } = await axios.delete<boolean>(
      `/api/draft/${draftId}/delete`,
    );
    return data;
  }
}
