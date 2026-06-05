import axios from "axios";
import {
  CreateDraftPayload,
  UpdateChaptersDraftPayload,
  UpdateLessonssDraftPayload,
} from "../schemas/draft.schema";
import { DraftEntity } from "@/domain/entities/draft.entity";

export async function CreateDraftService(
  payload: CreateDraftPayload,
): Promise<string> {
  const response = await axios.post<string>(`/api/draft/create`, payload);
  return response.data;
}

export async function GetDraft(draftId: string): Promise<DraftEntity> {
  const response = await axios.get<DraftEntity>(`/api/draft/get`, {
    params: { draftId: draftId },
  });

  return response.data;
}

export async function UpdateChapters(payload: UpdateChaptersDraftPayload) {
  const response = await axios.put<DraftEntity>(
    `/api/draft/update/chapter`,
    payload,
  );
  return response.data;
}

export async function UpdateLessons(payload: UpdateLessonssDraftPayload) {
  const response = await axios.put<DraftEntity>(
    `/api/draft/update/lesson`,
    payload,
  );
  return response.data;
}

export async function GenerateMatrix(draftId: string): Promise<boolean> {
  const response = await axios.put<boolean>(
    `/api/draft/generate-matrix`,
    {},
    {
      params: {
        draftId: draftId,
      },
    },
  );

  return response.data;
}

export async function GenerateMatrixDetails(draftId: string): Promise<boolean> {
  const response = await axios.put<boolean>(
    `/api/draft/generate-matrix-details`,
    {},
    {
      params: {
        draftId: draftId,
      },
    },
  );

  return response.data;
}

