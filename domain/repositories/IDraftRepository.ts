import {
  CreateDraftPayloadEntity,
  UpdateChaptersDraftPayloadEntity,
  DraftEntity,
  UpdateLessonsDraftPayloadEntity,
} from "../entities/draft.entity";

export interface IDraftRepository {
  createDraft(payload: CreateDraftPayloadEntity): Promise<string>;
  getDraft(draftId: string): Promise<DraftEntity>;
  updateChapters(payload: UpdateChaptersDraftPayloadEntity): Promise<boolean>;
  updateLessons(payload: UpdateLessonsDraftPayloadEntity): Promise<boolean>;
  generateMatrix(draftId: string): Promise<boolean>;
  generateMatrixDetails(draftId: string): Promise<boolean>;
}
