import {
  CreateDraftPayloadEntity,
  UpdateChaptersDraftPayloadEntity,
  DraftEntity,
  UpdateLessonsDraftPayloadEntity,
} from "../entities/draft.entity";

export interface IDraftRepository {
  createDraft(
    payload: CreateDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<string>;
  getDraft(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity>;
  updateChapters(
    payload: UpdateChaptersDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  updateLessons(
    payload: UpdateLessonsDraftPayloadEntity,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  generateMatrix(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  generateMatrixDetails(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
  getRecentDraft(
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity[]>;
  getAllUserDrafts(
    accessToken: string,
    refreshToken: string,
  ): Promise<DraftEntity[]>;
  deleteDraft(
    draftId: string,
    accessToken: string,
    refreshToken: string,
  ): Promise<boolean>;
}
