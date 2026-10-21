import {
  CreateDraftEntity,
  DraftEntity,
  UpdateDraftEntity,
} from "../entities/draft.entity";

export interface IDraftRepository {
  createDraft(payload: CreateDraftEntity, accessToken: string): Promise<string>;

  getDraft(draftId: string, accessToken: string): Promise<DraftEntity>;

  updateChapters(
    draftId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ): Promise<boolean>;

  updateLessons(
    draftId: string,
    chapterId: string,
    payload: UpdateDraftEntity,
    accessToken: string,
  ): Promise<boolean>;

  generateMatrix(draftId: string, accessToken: string): Promise<boolean>;

  generateMatrixDetails(draftId: string, accessToken: string): Promise<boolean>;

  getRecentDraft(accessToken: string): Promise<DraftEntity[]>;

  getAllUserDrafts(accessToken: string): Promise<DraftEntity[]>;

  deleteDraft(draftId: string, accessToken: string): Promise<boolean>;
}
