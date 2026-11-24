package com.wedu.exam_creation.chapter.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.wedu.exam_creation.chapter.infrastructure.document.*;
import com.wedu.exam_creation.chapter.infrastructure.mapper.ChapterMapper;
import com.wedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;
import com.wedu.exam_creation.common.exception.NotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

class ChapterRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final ChapterMapper mapper = mock(ChapterMapper.class);
    private final ChapterRepositoryImpl repository = new ChapterRepositoryImpl(mongo, mapper);
    private final Instant created = Instant.parse("2026-11-19T00:00:00Z");
    private final Instant updated = Instant.parse("2026-11-20T00:00:00Z");

    private BankStatDocument stat(int count) {
        return new BankStatDocument(
                "Dạng 1", List.of("Nhận biết"), List.of("Yêu cầu 1"), "Đúng sai", count);
    }

    private ChapterDocument document(String lessonName, List<BankStatDocument> stats) {
        return new ChapterDocument(
                "c1",
                "Vật lý",
                "Chương 1",
                new ArrayList<>(List.of(new LessonDataDocument("l1", lessonName, stats))),
                created,
                created);
    }

    private ChapterEntity prepareSave(ChapterDocument incoming, ChapterDocument existing) {
        ChapterEntity entity =
                new ChapterEntity(
                        null,
                        incoming.getSubject(),
                        incoming.getName(),
                        List.of(
                                new com.wedu.exam_creation.chapter.domain.entity.LessonDataEntity(
                                        null, incoming.getLessons().get(0).getName(), List.of())),
                        null,
                        null);
        when(mapper.toDocument(entity)).thenReturn(incoming);
        when(mongo.findOne(any(Query.class), eq(ChapterDocument.class))).thenReturn(existing);
        return entity;
    }

    @Test
    @DisplayName("trả về ID chương và bài đã lưu khi chương chưa tồn tại")
    void test_saveChapter_001() {
        ChapterDocument incoming = document("Bài mới", new ArrayList<>(List.of(stat(2))));
        incoming.setId(null);
        incoming.getLessons().get(0).setId(null);
        ChapterEntity entity = prepareSave(incoming, null);
        when(mongo.save(incoming))
                .thenAnswer(
                        call -> {
                            incoming.setId("created-chapter");
                            return incoming;
                        });
        SavedChapterResponse result;
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(updated);
            result = repository.saveChapter(entity);
        }
        assertThat(result.getChapterId()).isEqualTo("created-chapter");
        assertThat(result.getLessonId()).isEqualTo(incoming.getLessons().get(0).getId());
        assertThat(ObjectId.isValid(result.getLessonId())).isTrue();
        assertThat(incoming.getCreateAt()).isEqualTo(updated);
        assertThat(incoming.getUpdatedAt()).isEqualTo(updated);
        assertThat(incoming.getLessons().get(0).getBankStats()).containsExactly(stat(2));
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).findOne(query.capture(), eq(ChapterDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("name", "Chương 1"));
        verify(mongo).save(incoming);
    }

    @Test
    @DisplayName("trả về ID bài mới và giữ bài cũ khi chương có sẵn nhưng chưa có bài cùng tên")
    void test_saveChapter_002() {
        ChapterDocument incoming = document("Bài mới", new ArrayList<>(List.of(stat(2))));
        ChapterDocument existing = document("Bài cũ", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(incoming, existing);
        SavedChapterResponse result;
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(updated);
            result = repository.saveChapter(entity);
        }
        assertThat(result.getChapterId()).isEqualTo("c1");
        assertThat(ObjectId.isValid(result.getLessonId())).isTrue();
        assertThat(existing.getLessons())
                .extracting(LessonDataDocument::getName)
                .containsExactly("Bài cũ", "Bài mới");
        assertThat(existing.getLessons().get(0).getBankStats()).containsExactly(stat(4));
        assertThat(existing.getLessons().get(1).getId()).isEqualTo(result.getLessonId());
        assertThat(existing.getCreateAt()).isEqualTo(created);
        assertThat(existing.getUpdatedAt()).isEqualTo(updated);
        verify(mongo).save(existing);
    }

    @Test
    @DisplayName("trả về ID hiện có và cộng count khi tất cả metadata thống kê trùng khớp")
    void test_saveChapter_003() {
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(document("Bài 1", List.of(stat(2), stat(3))), existing);
        SavedChapterResponse result;
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(updated);
            result = repository.saveChapter(entity);
        }
        assertThat(result).isEqualTo(new SavedChapterResponse("c1", "l1"));
        assertThat(existing.getLessons()).hasSize(1);
        assertThat(existing.getLessons().get(0).getBankStats()).containsExactly(stat(9));
        assertThat(existing.getUpdatedAt()).isEqualTo(updated);
        assertThat(existing.getCreateAt()).isEqualTo(created);
        verify(mongo).save(existing);
    }

    @ParameterizedTest
    @ValueSource(strings = {"exerciseType", "questionType", "difficultyLevels", "learningOutcomes"})
    @DisplayName("trả về hai thống kê riêng khi một trường metadata khác nhau")
    void test_saveChapter_004(String field) {
        BankStatDocument different = stat(2);
        switch (field) {
            case "exerciseType" -> different.setExerciseType("Dạng 2");
            case "questionType" -> different.setQuestionType("Trả lời ngắn");
            case "difficultyLevels" -> different.setDifficultyLevels(List.of("Thông hiểu"));
            case "learningOutcomes" -> different.setLearningOutcomes(List.of("Yêu cầu 2"));
            default -> throw new IllegalArgumentException(field);
        }
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(document("Bài 1", List.of(different)), existing);
        assertThat(repository.saveChapter(entity)).isEqualTo(new SavedChapterResponse("c1", "l1"));
        assertThat(existing.getLessons().get(0).getBankStats()).containsExactly(stat(4), different);
        verify(mongo).save(existing);
    }

    @Test
    @DisplayName("trả về ID hiện có và không ghi khi bankStats mới rỗng")
    void test_saveChapter_005() {
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(document("Bài 1", List.of()), existing);
        assertThat(repository.saveChapter(entity)).isEqualTo(new SavedChapterResponse("c1", "l1"));
        assertThat(existing.getUpdatedAt()).isEqualTo(created);
        assertThat(existing.getLessons().get(0).getBankStats()).containsExactly(stat(4));
        verify(mongo, never()).save(any(ChapterDocument.class));
    }

    @Test
    @DisplayName("trả về ID hiện có và không ghi khi bankStats mới null")
    void test_saveChapter_006() {
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(document("Bài 1", null), existing);
        assertThat(repository.saveChapter(entity)).isEqualTo(new SavedChapterResponse("c1", "l1"));
        assertThat(existing.getUpdatedAt()).isEqualTo(created);
        assertThat(existing.getLessons().get(0).getBankStats()).containsExactly(stat(4));
        verify(mongo, never()).save(any(ChapterDocument.class));
    }

    @Test
    @DisplayName("trả về lỗi Mongo và không ghi khi truy vấn chương thất bại")
    void test_saveChapter_007() {
        ChapterEntity entity = prepareSave(document("Bài 1", List.of()), null);
        IllegalStateException failure = new IllegalStateException("lookup failed");
        when(mongo.findOne(any(Query.class), eq(ChapterDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.saveChapter(entity)).isSameAs(failure);
        verify(mongo, never()).save(any(ChapterDocument.class));
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi lưu chương mới thất bại")
    void test_saveChapter_008() {
        ChapterDocument incoming = document("Bài 1", List.of());
        ChapterEntity entity = prepareSave(incoming, null);
        IllegalStateException failure = new IllegalStateException("save failed");
        when(mongo.save(incoming)).thenThrow(failure);
        assertThatThrownBy(() -> repository.saveChapter(entity)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi lưu thống kê đã merge thất bại")
    void test_saveChapter_009() {
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        ChapterEntity entity = prepareSave(document("Bài 1", List.of(stat(2))), existing);
        IllegalStateException failure = new IllegalStateException("save failed");
        when(mongo.save(existing)).thenThrow(failure);
        assertThatThrownBy(() -> repository.saveChapter(entity)).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về các entity đúng thứ tự khi Mongo có nhiều chương")
    void test_getAll_010() {
        ChapterDocument first = document("Bài 1", List.of());
        ChapterDocument second = document("Bài 2", List.of());
        ChapterEntity firstEntity = new ChapterEntity();
        ChapterEntity secondEntity = new ChapterEntity();
        secondEntity.setId("c2");
        when(mongo.findAll(ChapterDocument.class)).thenReturn(List.of(first, second));
        when(mapper.toEntity(first)).thenReturn(firstEntity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        assertThat(repository.getAll()).containsExactly(firstEntity, secondEntity);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi Mongo chưa có chương")
    void test_getAll_011() {
        when(mongo.findAll(ChapterDocument.class)).thenReturn(List.of());
        assertThat(repository.getAll()).isEmpty();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi đọc toàn bộ chương thất bại")
    void test_getAll_012() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(mongo.findAll(ChapterDocument.class)).thenThrow(failure);
        assertThatThrownBy(repository::getAll).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về entity đã map khi tìm thấy đúng ID chương")
    void test_getById_013() {
        ChapterDocument document = document("Bài 1", List.of());
        ChapterEntity entity = new ChapterEntity();
        when(mongo.findById("c1", ChapterDocument.class)).thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(entity);
        assertThat(repository.getById("c1")).isSameAs(entity);
    }

    @Test
    @DisplayName("trả về NotFoundException khi ID chương không tồn tại")
    void test_getById_014() {
        assertThatThrownBy(() -> repository.getById("missing"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tồn tại chương này");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi đọc theo ID thất bại")
    void test_getById_015() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(mongo.findById("c1", ChapterDocument.class)).thenThrow(failure);
        assertThatThrownBy(() -> repository.getById("c1")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về entity theo thứ tự Mongo khi chỉ tìm thấy một phần ID yêu cầu")
    void test_getByIds_016() {
        ChapterDocument document = document("Bài 1", List.of());
        ChapterDocument second = document("Bài 2", List.of());
        second.setId("c2");
        ChapterEntity entity = new ChapterEntity();
        entity.setId("c1");
        ChapterEntity secondEntity = new ChapterEntity();
        secondEntity.setId("c2");
        when(mongo.find(any(Query.class), eq(ChapterDocument.class)))
                .thenReturn(List.of(second, document));
        when(mapper.toEntity(document)).thenReturn(entity);
        when(mapper.toEntity(second)).thenReturn(secondEntity);
        assertThat(repository.getByIds(List.of("c1", "missing", "c2")))
                .containsExactly(secondEntity, entity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(ChapterDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(
                        new Document("_id", new Document("$in", List.of("c1", "missing", "c2"))));
    }

    @Test
    @DisplayName("trả về NotFoundException khi không ID nào tồn tại")
    void test_getByIds_017() {
        when(mongo.find(any(Query.class), eq(ChapterDocument.class))).thenReturn(List.of());
        assertThatThrownBy(() -> repository.getByIds(List.of("missing")))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tồn tại chương này");
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về NotFoundException khi danh sách ID rỗng không có kết quả")
    void test_getByIds_018() {
        when(mongo.find(any(Query.class), eq(ChapterDocument.class))).thenReturn(List.of());
        assertThatThrownBy(() -> repository.getByIds(List.of()))
                .isInstanceOf(NotFoundException.class);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(ChapterDocument.class));
        assertThat(query.getValue().getQueryObject())
                .isEqualTo(new Document("_id", new Document("$in", List.of())));
    }

    @Test
    @DisplayName("trả về lỗi Mongo khi truy vấn danh sách ID thất bại")
    void test_getByIds_019() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(mongo.find(any(Query.class), eq(ChapterDocument.class))).thenThrow(failure);
        assertThatThrownBy(() -> repository.getByIds(List.of("c1"))).isSameAs(failure);
    }

    @Test
    @DisplayName(
            "trả về ID bài trùng tên và chỉ gộp thống kê của bài đó khi chương đã có nhiều bài")
    void test_saveChapter_020() {
        ChapterDocument existing = document("Bài 1", new ArrayList<>(List.of(stat(4))));
        existing.getLessons()
                .add(new LessonDataDocument("l2", "Bài 2", new ArrayList<>(List.of(stat(6)))));
        ChapterEntity entity = prepareSave(document("Bài 2", List.of(stat(2))), existing);

        SavedChapterResponse result;
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(updated);
            result = repository.saveChapter(entity);
        }

        assertThat(result).isEqualTo(new SavedChapterResponse("c1", "l2"));
        assertThat(existing.getLessons())
                .containsExactly(
                        new LessonDataDocument("l1", "Bài 1", List.of(stat(4))),
                        new LessonDataDocument("l2", "Bài 2", List.of(stat(8))));
        assertThat(existing.getUpdatedAt()).isEqualTo(updated);
        verify(mongo).save(existing);
    }
}
