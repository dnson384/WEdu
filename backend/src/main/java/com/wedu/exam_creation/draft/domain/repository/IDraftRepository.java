package com.wedu.exam_creation.draft.domain.repository;

import com.wedu.exam_creation.draft.domain.entity.DraftEntity;
import com.wedu.exam_creation.draft.domain.payload.UpdateChaptersPayload;
import com.wedu.exam_creation.draft.domain.payload.UpdateLessonsPayload;
import com.wedu.exam_creation.draft.domain.payload.UpdateMatrixDetailsPayload;
import com.wedu.exam_creation.draft.domain.payload.UpdateMatrixPayload;

import java.util.List;

public interface IDraftRepository {
    String createDraft(DraftEntity draft);

    DraftEntity getDraft(String draftId, String userId);

    boolean updateChapters(UpdateChaptersPayload payload);

    boolean updateLessons(UpdateLessonsPayload payload);

    boolean updateMatrix(List<UpdateMatrixPayload> payloads);

    boolean updateMatrixDetails(List<UpdateMatrixDetailsPayload> payloads);

    boolean deleteDraft(String draftId, String userId);

    List<DraftEntity> getRecentDraft(String userId);

    List<DraftEntity> getAllUserDrafts(String userId);
}
