package com.wedu.exam_creation.draft.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.draft.domain.entity.*;
import com.wedu.exam_creation.draft.domain.payload.*;
import com.wedu.exam_creation.draft.infrastructure.document.*;
import com.wedu.exam_creation.draft.infrastructure.mapper.DraftMapper;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;

class DraftRepositoryImplTest {
    private static final String DRAFT = "000000000000000000000001";
    private static final String OWNER = "000000000000000000000002";
    private static final String CHAPTER = "000000000000000000000003";
    private static final String LESSON = "000000000000000000000004";
    private static final String ADDED = "000000000000000000000005";
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final DraftMapper mapper = mock(DraftMapper.class);
    private final DraftRepositoryImpl repo = new DraftRepositoryImpl(mongo, mapper);

    private DraftEntity existing() {
        DraftEntity entity =
                new DraftEntity(
                        DRAFT,
                        OWNER,
                        "Đề",
                        3,
                        List.of("Đúng sai"),
                        List.of(
                                new ChapterDraftEntity(
                                        CHAPTER,
                                        "Chương",
                                        List.of(
                                                new LessonDraftEntity(
                                                        LESSON, "Bài", List.of(), List.of())))),
                        null,
                        null);
        DraftDocument document = Mappers.getMapper(DraftMapper.class).toDocument(entity);
        when(mongo.findOne(any(Query.class), eq(DraftDocument.class))).thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(entity);
        when(mapper.toDocument(entity)).thenReturn(document);
        return entity;
    }

    private Query queryForUpdate() {
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).updateFirst(query.capture(), any(Update.class), eq(DraftDocument.class));
        return query.getValue();
    }

    private Update capturedUpdate() {
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class), update.capture(), eq(DraftDocument.class));
        return update.getValue();
    }

    @Test
    @DisplayName("trả về ID được Mongo lưu khi tạo bản nháp hợp lệ")
    void test_createDraft_001() {

        DraftEntity entity = new DraftEntity();
        DraftDocument document = new DraftDocument();
        DraftDocument saved = new DraftDocument();
        saved.setId(DRAFT);
        when(mapper.toDocument(entity)).thenReturn(document);
        when(mongo.insert(document)).thenReturn(saved);
        assertThat(repo.createDraft(entity)).isEqualTo(DRAFT);
        verify(mongo).insert(document);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi insert thất bại")
    void test_createDraft_002() {

        DraftEntity entity = new DraftEntity();
        DraftDocument document = new DraftDocument();
        when(mapper.toDocument(entity)).thenReturn(document);
        when(mongo.insert(document)).thenThrow(new IllegalStateException("insert failed"));
        assertThatThrownBy(() -> repo.createDraft(entity))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về entity khi truy vấn có cả draft ID và owner ID")
    void test_getDraft_003() {

        DraftEntity entity = existing();
        assertThat(repo.getDraft(DRAFT, OWNER)).isSameAs(entity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(DraftDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(
                        new Document(
                                "$and",
                                List.of(
                                        new Document("_id", DRAFT),
                                        new Document("userId", OWNER))));
    }

    @Test
    @DisplayName("trả về NotFoundException khi không có dữ liệu khớp draft và owner")
    void test_getDraft_004() {

        assertThatThrownBy(() -> repo.getDraft(DRAFT, OWNER))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Bản nháp không tồn tại");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi đọc dữ liệu thất bại")
    void test_getDraft_005() {

        when(mongo.findOne(any(Query.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("read failed"));
        assertThatThrownBy(() -> repo.getDraft(DRAFT, OWNER))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về true và chỉ thêm mục mới khi payload gồm mục đã tồn tại")
    void test_updateChapters_006() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(
                        repo.updateChapters(
                                new UpdateChaptersPayload(
                                        DRAFT,
                                        OWNER,
                                        List.of(
                                                new UpdateParam(CHAPTER, "Cũ"),
                                                new UpdateParam(ADDED, "Mới")),
                                        List.of())))
                .isTrue();
        assertThat(capturedUpdate().getUpdateObject())
                .isEqualTo(
                        new Update()
                                .push("chapters")
                                .each(List.of(new ChapterDraftDocument(ADDED, "Mới", List.of())))
                                .getUpdateObject());
        assertThat(queryForUpdate().getQueryObject()).containsEntry("_id", new ObjectId(DRAFT));
    }

    @Test
    @DisplayName("trả về false và không ghi khi payload rỗng hoặc chỉ thêm mục đã tồn tại")
    void test_updateChapters_007() {

        existing();
        assertThat(
                        repo.updateChapters(
                                new UpdateChaptersPayload(DRAFT, OWNER, List.of(), List.of())))
                .isFalse();
        assertThat(repo.updateChapters(new UpdateChaptersPayload(DRAFT, OWNER, null, null)))
                .isFalse();
        assertThat(
                        repo.updateChapters(
                                new UpdateChaptersPayload(
                                        DRAFT,
                                        OWNER,
                                        List.of(new UpdateParam(CHAPTER, "Cũ")),
                                        List.of())))
                .isFalse();
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về điều kiện pull đúng ID khi xóa mục được chọn")
    void test_updateChapters_008() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(
                        repo.updateChapters(
                                new UpdateChaptersPayload(
                                        DRAFT, OWNER, List.of(), List.of(CHAPTER))))
                .isTrue();
        assertThat(capturedUpdate().getUpdateObject())
                .containsEntry(
                        "$pull",
                        new Document(
                                "chapters",
                                new Document(
                                        "_id",
                                        new Document("$in", List.of(new ObjectId(CHAPTER))))));
    }

    @Test
    @DisplayName("trả về false khi Mongo không sửa document nào")
    void test_updateChapters_009() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        assertThat(
                        repo.updateChapters(
                                new UpdateChaptersPayload(
                                        DRAFT,
                                        OWNER,
                                        List.of(new UpdateParam(ADDED, "Mới")),
                                        List.of())))
                .isFalse();
    }

    @Test
    @DisplayName("trả về NotFoundException và không ghi khi không tìm thấy draft của owner")
    void test_updateChapters_010() {

        assertThatThrownBy(
                        () ->
                                repo.updateChapters(
                                        new UpdateChaptersPayload(
                                                DRAFT, OWNER, List.of(), List.of())))
                .isInstanceOf(NotFoundException.class);
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về IllegalArgumentException và không ghi khi ID cần xóa sai định dạng")
    void test_updateChapters_011() {

        existing();
        assertThatThrownBy(
                        () ->
                                repo.updateChapters(
                                        new UpdateChaptersPayload(
                                                DRAFT, OWNER, List.of(), List.of("invalid"))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi update thất bại")
    void test_updateChapters_012() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("write failed"));
        assertThatThrownBy(
                        () ->
                                repo.updateChapters(
                                        new UpdateChaptersPayload(
                                                DRAFT,
                                                OWNER,
                                                List.of(new UpdateParam(ADDED, "Mới")),
                                                List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về true và chỉ thêm mục mới khi payload gồm mục đã tồn tại")
    void test_updateLessons_013() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(
                        repo.updateLessons(
                                new UpdateLessonsPayload(
                                        DRAFT,
                                        OWNER,
                                        CHAPTER,
                                        List.of(
                                                new UpdateParam(LESSON, "Cũ"),
                                                new UpdateParam(ADDED, "Mới")),
                                        List.of())))
                .isTrue();
        assertThat(capturedUpdate().getUpdateObject())
                .isEqualTo(
                        new Update()
                                .push("chapters.$.lessons")
                                .each(
                                        List.of(
                                                new LessonDraftDocument(
                                                        ADDED, "Mới", List.of(), List.of())))
                                .getUpdateObject());
        assertThat(queryForUpdate().getQueryObject()).containsEntry("_id", new ObjectId(DRAFT));
    }

    @Test
    @DisplayName("trả về false và không ghi khi payload rỗng hoặc chỉ thêm mục đã tồn tại")
    void test_updateLessons_014() {

        existing();
        assertThat(
                        repo.updateLessons(
                                new UpdateLessonsPayload(
                                        DRAFT, OWNER, CHAPTER, List.of(), List.of())))
                .isFalse();
        assertThat(repo.updateLessons(new UpdateLessonsPayload(DRAFT, OWNER, CHAPTER, null, null)))
                .isFalse();
        assertThat(
                        repo.updateLessons(
                                new UpdateLessonsPayload(
                                        DRAFT,
                                        OWNER,
                                        CHAPTER,
                                        List.of(new UpdateParam(LESSON, "Cũ")),
                                        List.of())))
                .isFalse();
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về điều kiện pull đúng ID khi xóa mục được chọn")
    void test_updateLessons_015() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(
                        repo.updateLessons(
                                new UpdateLessonsPayload(
                                        DRAFT, OWNER, CHAPTER, List.of(), List.of(LESSON))))
                .isTrue();
        assertThat(capturedUpdate().getUpdateObject())
                .containsEntry(
                        "$pull",
                        new Document(
                                "chapters.$.lessons",
                                new Document(
                                        "_id",
                                        new Document("$in", List.of(new ObjectId(LESSON))))));
    }

    @Test
    @DisplayName("trả về false khi Mongo không sửa document nào")
    void test_updateLessons_016() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 0L, null));
        assertThat(
                        repo.updateLessons(
                                new UpdateLessonsPayload(
                                        DRAFT,
                                        OWNER,
                                        CHAPTER,
                                        List.of(new UpdateParam(ADDED, "Mới")),
                                        List.of())))
                .isFalse();
    }

    @Test
    @DisplayName("trả về NotFoundException và không ghi khi không tìm thấy draft của owner")
    void test_updateLessons_017() {

        assertThatThrownBy(
                        () ->
                                repo.updateLessons(
                                        new UpdateLessonsPayload(
                                                DRAFT, OWNER, CHAPTER, List.of(), List.of())))
                .isInstanceOf(NotFoundException.class);
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về IllegalArgumentException và không ghi khi ID cần xóa sai định dạng")
    void test_updateLessons_018() {

        existing();
        assertThatThrownBy(
                        () ->
                                repo.updateLessons(
                                        new UpdateLessonsPayload(
                                                DRAFT,
                                                OWNER,
                                                CHAPTER,
                                                List.of(),
                                                List.of("invalid"))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi update thất bại")
    void test_updateLessons_019() {

        existing();
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("write failed"));
        assertThatThrownBy(
                        () ->
                                repo.updateLessons(
                                        new UpdateLessonsPayload(
                                                DRAFT,
                                                OWNER,
                                                CHAPTER,
                                                List.of(new UpdateParam(ADDED, "Mới")),
                                                List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về NotFoundException và không ghi khi chương không thuộc draft")
    void test_updateLessons_020() {

        existing();
        assertThatThrownBy(
                        () ->
                                repo.updateLessons(
                                        new UpdateLessonsPayload(
                                                DRAFT, OWNER, ADDED, List.of(), List.of())))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Chương không tồn tại trong bản nháp");
        verify(mongo, never())
                .updateFirst(any(Query.class), any(Update.class), eq(DraftDocument.class));
    }

    @Test
    @DisplayName("trả về true cùng cập nhật đúng chapter lesson và dữ liệu khi bulk thành công")
    void test_updateMatrix_021() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        UpdateMatrixPayload payload =
                new UpdateMatrixPayload(
                        DRAFT,
                        OWNER,
                        CHAPTER,
                        LESSON,
                        List.of(new MatrixItemEntity("Đúng sai", "Nhận biết", 3)));
        assertThat(repo.updateMatrix(List.of(payload))).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(bulk).updateOne(query.capture(), update.capture());
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", new ObjectId(DRAFT));
        assertThat(update.getValue().getUpdateObject())
                .containsEntry(
                        "$set",
                        new Document(
                                "chapters.$[chapter].lessons.$[lesson].matrix",
                                payload.getMatrix()));
        assertThat(update.getValue().getArrayFilters())
                .extracting(filter -> filter.asDocument())
                .containsExactly(
                        new Document("chapter._id", new ObjectId(CHAPTER)),
                        new Document("lesson._id", new ObjectId(LESSON)));
        verify(bulk).execute();
    }

    @Test
    @DisplayName("trả về true và không gọi Mongo khi payload null hoặc rỗng")
    void test_updateMatrix_022() {

        assertThat(repo.updateMatrix(null)).isTrue();
        assertThat(repo.updateMatrix(List.of())).isTrue();
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi thực thi bulk thất bại")
    void test_updateMatrix_023() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        when(bulk.execute()).thenThrow(new IllegalStateException("bulk failed"));
        assertThatThrownBy(
                        () ->
                                repo.updateMatrix(
                                        List.of(
                                                new UpdateMatrixPayload(
                                                        DRAFT, OWNER, CHAPTER, LESSON, List.of()))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về IllegalArgumentException và không execute khi ID sai định dạng")
    void test_updateMatrix_024() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        assertThatThrownBy(
                        () ->
                                repo.updateMatrix(
                                        List.of(
                                                new UpdateMatrixPayload(
                                                        "invalid", OWNER, CHAPTER, LESSON,
                                                        List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bulk, never()).execute();
    }

    @Test
    @DisplayName("trả về true cùng cập nhật đúng chapter lesson và dữ liệu khi bulk thành công")
    void test_updateMatrixDetails_025() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        UpdateMatrixDetailsPayload payload =
                new UpdateMatrixDetailsPayload(
                        DRAFT,
                        OWNER,
                        CHAPTER,
                        LESSON,
                        List.of(
                                new MatrixDetailItemEntity(
                                        "Dạng", "Nhận biết", "Kiến thức", "Đúng sai", 3)));
        assertThat(repo.updateMatrixDetails(List.of(payload))).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(bulk).updateOne(query.capture(), update.capture());
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", new ObjectId(DRAFT));
        assertThat(update.getValue().getUpdateObject())
                .containsEntry(
                        "$set",
                        new Document(
                                "chapters.$[chapter].lessons.$[lesson].matrixDetails",
                                payload.getMatrixDetails()));
        assertThat(update.getValue().getArrayFilters())
                .extracting(filter -> filter.asDocument())
                .containsExactly(
                        new Document("chapter._id", new ObjectId(CHAPTER)),
                        new Document("lesson._id", new ObjectId(LESSON)));
        verify(bulk).execute();
    }

    @Test
    @DisplayName("trả về true và không gọi Mongo khi payload null hoặc rỗng")
    void test_updateMatrixDetails_026() {

        assertThat(repo.updateMatrixDetails(null)).isTrue();
        assertThat(repo.updateMatrixDetails(List.of())).isTrue();
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi thực thi bulk thất bại")
    void test_updateMatrixDetails_027() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        when(bulk.execute()).thenThrow(new IllegalStateException("bulk failed"));
        assertThatThrownBy(
                        () ->
                                repo.updateMatrixDetails(
                                        List.of(
                                                new UpdateMatrixDetailsPayload(
                                                        DRAFT, OWNER, CHAPTER, LESSON, List.of()))))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về IllegalArgumentException và không execute khi ID sai định dạng")
    void test_updateMatrixDetails_028() {

        BulkOperations bulk = mock(BulkOperations.class);
        when(mongo.bulkOps(BulkOperations.BulkMode.UNORDERED, DraftDocument.class))
                .thenReturn(bulk);
        assertThatThrownBy(
                        () ->
                                repo.updateMatrixDetails(
                                        List.of(
                                                new UpdateMatrixDetailsPayload(
                                                        "invalid", OWNER, CHAPTER, LESSON,
                                                        List.of()))))
                .isInstanceOf(IllegalArgumentException.class);
        verify(bulk, never()).execute();
    }

    @Test
    @DisplayName("trả về true và điều kiện cả draft lẫn owner khi Mongo xóa một document")
    void test_deleteDraft_029() {

        when(mongo.remove(any(Query.class), eq(DraftDocument.class)))
                .thenReturn(DeleteResult.acknowledged(1));
        assertThat(repo.deleteDraft(DRAFT, OWNER)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(DraftDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(
                        new Document(
                                "$and",
                                List.of(
                                        new Document("_id", new ObjectId(DRAFT)),
                                        new Document("userId", new ObjectId(OWNER)))));
    }

    @Test
    @DisplayName("trả về false khi không có draft khớp owner")
    void test_deleteDraft_030() {

        when(mongo.remove(any(Query.class), eq(DraftDocument.class)))
                .thenReturn(DeleteResult.acknowledged(0));
        assertThat(repo.deleteDraft(DRAFT, OWNER)).isFalse();
    }

    @Test
    @DisplayName("trả về InternalServerException khi xóa vượt một document")
    void test_deleteDraft_031() {

        when(mongo.remove(any(Query.class), eq(DraftDocument.class)))
                .thenReturn(DeleteResult.acknowledged(2));
        assertThatThrownBy(() -> repo.deleteDraft(DRAFT, OWNER))
                .isInstanceOf(InternalServerException.class);
    }

    @Test
    @DisplayName("trả về IllegalArgumentException trước khi gọi Mongo khi ID sai định dạng")
    void test_deleteDraft_032() {

        assertThatThrownBy(() -> repo.deleteDraft("invalid", OWNER))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(mongo);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi remove thất bại")
    void test_deleteDraft_033() {

        when(mongo.remove(any(Query.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("delete failed"));
        assertThatThrownBy(() -> repo.deleteDraft(DRAFT, OWNER))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("trả về danh sách đã map theo owner và giảm dần thời gian cập nhật khi có dữ liệu")
    void test_getRecentDraft_034() {

        DraftDocument first = new DraftDocument();
        first.setId(DRAFT);
        DraftDocument second = new DraftDocument();
        second.setId(ADDED);
        DraftEntity firstEntity = new DraftEntity();
        firstEntity.setId(DRAFT);
        DraftEntity secondEntity = new DraftEntity();
        secondEntity.setId(ADDED);
        when(mongo.find(any(Query.class), eq(DraftDocument.class)))
                .thenReturn(List.of(first, second));
        when(mapper.toEntity(first)).thenReturn(firstEntity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        assertThat(repo.getRecentDraft(OWNER)).containsExactly(firstEntity, secondEntity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(DraftDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("userId", OWNER);
        assertThat(query.getValue().getSortObject()).isEqualTo(new Document("updatedAt", -1));
        assertThat(query.getValue().getLimit()).isEqualTo(5);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có nháp")
    void test_getRecentDraft_035() {

        when(mongo.find(any(Query.class), eq(DraftDocument.class))).thenReturn(List.of());
        assertThat(repo.getRecentDraft(OWNER)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi truy vấn thất bại")
    void test_getRecentDraft_036() {

        when(mongo.find(any(Query.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("read failed"));
        assertThatThrownBy(() -> repo.getRecentDraft(OWNER))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về danh sách đã map theo owner và giảm dần thời gian cập nhật khi có dữ liệu")
    void test_getAllUserDrafts_037() {

        DraftDocument first = new DraftDocument();
        first.setId(DRAFT);
        DraftDocument second = new DraftDocument();
        second.setId(ADDED);
        DraftEntity firstEntity = new DraftEntity();
        firstEntity.setId(DRAFT);
        DraftEntity secondEntity = new DraftEntity();
        secondEntity.setId(ADDED);
        when(mongo.find(any(Query.class), eq(DraftDocument.class)))
                .thenReturn(List.of(first, second));
        when(mapper.toEntity(first)).thenReturn(firstEntity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        assertThat(repo.getAllUserDrafts(OWNER)).containsExactly(firstEntity, secondEntity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(DraftDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("userId", OWNER);
        assertThat(query.getValue().getSortObject()).isEqualTo(new Document("updatedAt", -1));
        assertThat(query.getValue().getLimit()).isEqualTo(0);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có nháp")
    void test_getAllUserDrafts_038() {

        when(mongo.find(any(Query.class), eq(DraftDocument.class))).thenReturn(List.of());
        assertThat(repo.getAllUserDrafts(OWNER)).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi truy vấn thất bại")
    void test_getAllUserDrafts_039() {

        when(mongo.find(any(Query.class), eq(DraftDocument.class)))
                .thenThrow(new IllegalStateException("read failed"));
        assertThatThrownBy(() -> repo.getAllUserDrafts(OWNER))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
    }
}
