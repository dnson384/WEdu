package com.fckedu.exam_creation.draft.infrastructure.repository;

import com.fckedu.exam_creation.common.exception.NotFoundException;
import com.fckedu.exam_creation.draft.domain.entity.ChapterDraftEntity;
import com.fckedu.exam_creation.draft.domain.entity.DraftEntity;
import com.fckedu.exam_creation.draft.domain.payload.UpdateChaptersPayload;
import com.fckedu.exam_creation.draft.domain.payload.UpdateLessonsPayload;
import com.fckedu.exam_creation.draft.domain.payload.UpdateMatrixDetailsPayload;
import com.fckedu.exam_creation.draft.domain.payload.UpdateMatrixPayload;
import com.fckedu.exam_creation.draft.domain.repository.IDraftRepository;
import com.fckedu.exam_creation.draft.infrastructure.document.ChapterDraftDocument;
import com.fckedu.exam_creation.draft.infrastructure.document.DraftDocument;
import com.fckedu.exam_creation.draft.infrastructure.document.LessonDraftDocument;
import com.fckedu.exam_creation.draft.infrastructure.mapper.DraftMapper;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class DraftRepositoryImpl implements IDraftRepository {
    private final MongoTemplate mongoTemplate;
    private final DraftMapper draftMapper;

    public DraftRepositoryImpl(MongoTemplate mongoTemplate, DraftMapper draftMapper) {
        this.mongoTemplate = mongoTemplate;
        this.draftMapper = draftMapper;
    }

    @Override
    public String createDraft(DraftEntity draft) {
        DraftDocument draftDocument = draftMapper.toDocument(draft);
        DraftDocument result = mongoTemplate.insert(draftDocument);
        return result.getId();
    }

    @Override
    public DraftEntity getDraft(String draftId, String userId) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("_id").is(draftId),
                Criteria.where("userId").is(userId)
        );

        Query query = new Query(criteria);

        DraftDocument draft = mongoTemplate.findOne(query, DraftDocument.class);

        if (draft == null) {
            throw new NotFoundException("Bản nháp không tồn tại");
        }

        return draftMapper.toEntity(draft);
    }

    @Override
    public boolean updateChapters(UpdateChaptersPayload payload) {
        DraftEntity draft = getDraft(payload.getDraftId(), payload.getUserId());

        List<String> chapterIds = draft.getChapters().stream().map(ChapterDraftEntity::getId).toList();

        Update update = new Update();
        boolean hasUpdates = false;

        // Thêm
        if (payload.getAdd() != null && !payload.getAdd().isEmpty()) {
            List<ChapterDraftDocument> chaptersToAdd = payload.getAdd().stream()
                    .filter(chapter -> !chapterIds.contains(chapter.getId()))
                    .map(chapter -> new ChapterDraftDocument(
                            chapter.getId(),
                            chapter.getName(),
                            new ArrayList<>()
                    ))
                    .toList();

            if (!chaptersToAdd.isEmpty()) {
                update.push("chapters").each(chaptersToAdd);
                hasUpdates = true;
            }
        }

        // Xóa
        if (payload.getDel() != null && !payload.getDel().isEmpty()) {
            List<ObjectId> objectIdsToDelete = payload.getDel().stream()
                    .map(ObjectId::new)
                    .toList();

            Document inClause = new Document("$in", objectIdsToDelete);
            Document pullCondition = new Document("_id", inClause);

            update.pull("chapters", pullCondition);
            hasUpdates = true;
        }

        if (!hasUpdates) {
            return false;
        }

        Query query = new Query(Criteria.where("_id").is(new ObjectId(payload.getDraftId())));
        UpdateResult result = mongoTemplate.updateFirst(query, update, DraftDocument.class);

        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateLessons(UpdateLessonsPayload payload) {
        DraftDocument draft = draftMapper.toDocument(getDraft(payload.getDraftId(), payload.getUserId()));

        ChapterDraftDocument curChapter = draft.getChapters().stream()
                .filter(c -> c.getId().equals(payload.getChapterId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Chương không tồn tại trong bản nháp"));

        List<String> curLessonIds = curChapter.getLessons().stream()
                .map(LessonDraftDocument::getId)
                .toList();

        Update update = new Update();
        boolean hasChanges = false;

        // Thêm
        if (payload.getAdd() != null && !payload.getAdd().isEmpty()) {
            List<LessonDraftDocument> lessonsToAdd = payload.getAdd().stream()
                    .filter(lesson -> !curLessonIds.contains(lesson.getId()))
                    .map(lesson -> new LessonDraftDocument(
                            lesson.getId(),
                            lesson.getName(),
                            new ArrayList<>(),
                            new ArrayList<>()
                    ))
                    .toList();

            if (!lessonsToAdd.isEmpty()) {
                update.push("chapters.$.lessons").each(lessonsToAdd);
                hasChanges = true;
            }
        }

        // Xóa
        if (payload.getDel() != null && !payload.getDel().isEmpty()) {
            List<ObjectId> objectIdsToDelete = payload.getDel().stream().map(ObjectId::new).toList();

            Document inClause = new Document("$in", objectIdsToDelete);
            Document pullCondition = new Document("_id", inClause);

            update.pull("chapters.$.lessons", pullCondition);
            hasChanges = true;
        }

        if (!hasChanges) {
            return false;
        }

        Query query = new Query(
                Criteria.where("_id").is(new ObjectId(payload.getDraftId()))
                        .and("chapters.id").is(new ObjectId(payload.getChapterId()))
        );

        UpdateResult result = mongoTemplate.updateFirst(query, update, DraftDocument.class);
        return result.getModifiedCount() > 0;
    }

    @Override
    public boolean updateMatrix(List<UpdateMatrixPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return true;
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class);

        for (UpdateMatrixPayload payload : payloads) {
            Query query = new Query(Criteria.where("_id").is(new ObjectId(payload.getDraftId())));
            Update update = new Update();

            update.set("chapters.$[chapter].lessons.$[lesson].matrix", payload.getMatrix());
            update.filterArray(Criteria.where("chapter._id").is(new ObjectId(payload.getChapterId())));
            update.filterArray(Criteria.where("lesson._id").is(new ObjectId(payload.getLessonId())));


            bulkOps.updateOne(query, update);
        }

        bulkOps.execute();
        return true;
    }

    @Override
    public boolean updateMatrixDetails(List<UpdateMatrixDetailsPayload> payloads) {
        if (payloads == null || payloads.isEmpty()) {
            return true;
        }

        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class);

        for (UpdateMatrixDetailsPayload payload : payloads) {
            Query query = new Query(Criteria.where("_id").is(new ObjectId(payload.getDraftId())));
            Update update = new Update();

            update.set("chapters.$[chapter].lessons.$[lesson].matrixDetails", payload.getMatrixDetails());
            update.filterArray(Criteria.where("chapter._id").is(new ObjectId(payload.getChapterId())));
            update.filterArray(Criteria.where("lesson._id").is(new ObjectId(payload.getLessonId())));

            bulkOps.updateOne(query, update);
        }

        bulkOps.execute();
        return true;
    }

    @Override
    public void deleteDraft(String draftId, String userId) {
        Criteria criteria = new Criteria();
        criteria.andOperator(
                Criteria.where("_id").is(new ObjectId(draftId)),
                Criteria.where("userId").is(new ObjectId(userId))
        );

        Query query = new Query(criteria);

        DeleteResult result = mongoTemplate.remove(query, DraftDocument.class);
        result.getDeletedCount();
    }

    @Override
    public List<DraftEntity> getRecentDraft(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"))
                .limit(5);

        List<DraftDocument> draftDocuments = mongoTemplate.find(query, DraftDocument.class);

        return draftDocuments.stream().map(draftMapper::toEntity).toList();
    }

    @Override
    public List<DraftEntity> getAllUserDrafts(String userId) {
        Query query = new Query(Criteria.where("userId").is(userId))
                .with(Sort.by(Sort.Direction.DESC, "updatedAt"));
        List<DraftDocument> draftDocuments = mongoTemplate.find(query, DraftDocument.class);

        return draftDocuments.stream().map(draftMapper::toEntity).toList();
    }
}
