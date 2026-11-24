package com.wedu.exam_creation.question.infrastructure.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.entity.VariablesEntity;
import com.wedu.exam_creation.question.infrastructure.document.ContentDocument;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.document.VariablesDocument;
import com.wedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class QuestionRepositoryImplTest {
    @Test
    @DisplayName("trả về trang kích thước 10 và thứ tự ổn định khi lọc câu chờ duyệt")
    void test_findPending_001() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        when(mongo.count(any(Query.class), eq(QuestionDocument.class))).thenReturn(19L);
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());
        List<QuestionEntity> result =
                repository.findPending(1, "a.*", "Đúng sai", List.of("chapter"), List.of("lesson"));
        assertThat(result).isEmpty();
        assertThat(
                        repository.countPending(
                                "a.*", "Đúng sai", List.of("chapter"), List.of("lesson")))
                .isEqualTo(19);
        ArgumentCaptor<Query> count = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Query> find = ArgumentCaptor.forClass(Query.class);
        verify(mongo).count(count.capture(), eq(QuestionDocument.class));
        verify(mongo).find(find.capture(), eq(QuestionDocument.class));
        assertThat(find.getValue().getQueryObject().toJson())
                .isEqualTo(count.getValue().getQueryObject().toJson());
        assertThat(find.getValue().getQueryObject().toJson())
                .contains(
                        "PENDING_REVIEW",
                        "Đúng sai",
                        "chapterId",
                        "lessonId",
                        "question.template",
                        "\\\\Qa.*\\\\E");
        assertThat(count.getValue().getLimit()).isZero();
        assertThat(count.getValue().getSkip()).isZero();
        assertThat(find.getValue().getLimit()).isEqualTo(10);
        assertThat(find.getValue().getSkip()).isEqualTo(10);
        assertThat(find.getValue().getSortObject())
                .containsEntry("createdAt", -1)
                .containsEntry("_id", -1);
    }

    @Test
    @DisplayName("trả về trang rỗng khi hàng chờ không có câu hỏi")
    void test_findPending_002() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        assertThat(repository.findPending(0, "", "", List.of(), List.of())).isEmpty();
        assertThat(repository.countPending("", "", List.of(), List.of())).isZero();
    }

    @Test
    @DisplayName(
            "trả về true và chỉ đổi nội dung cùng phiên bản khi câu vẫn đang chờ và đúng phiên bản")
    void test_updatePendingContent_003() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        Instant version = Instant.parse("2026-10-29T00:00:00Z");
        ContentEntity content =
                new ContentEntity(
                        "<img_0>", new VariablesEntity(Map.of(), Map.of("img_0", "media/new.png")));
        ContentDocument mapped =
                new ContentDocument(
                        "<img_0>",
                        new VariablesDocument(Map.of(), Map.of("img_0", "media/new.png")));
        when(mapper.toDocument(
                        QuestionEntity.builder().question(content).options(List.of()).build()))
                .thenReturn(QuestionDocument.builder().question(mapped).build());
        Instant now = version.plusSeconds(1);
        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(now);
            assertThat(repository.updatePendingContent("id", content, List.of(), version)).isTrue();
        }
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(query.capture(), update.capture(), eq(QuestionDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "id")
                .containsEntry("status", "PENDING_REVIEW")
                .containsEntry("updatedAt", version);
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields)
                .containsOnlyKeys("question", "options", "updatedAt")
                .containsEntry("question", mapped)
                .containsEntry("options", List.of())
                .containsEntry("updatedAt", now);
    }

    @Test
    @DisplayName("trả về false khi không có câu chờ duyệt khớp phiên bản")
    void test_updatePendingContent_004() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        assertThat(repository.updatePendingContent("id", null, List.of(), null)).isFalse();
    }

    @Test
    @DisplayName(
            "trả về true và chỉ đổi trạng thái cùng phiên bản khi kết thúc duyệt không thay nội dung")
    void test_updatePendingState_005() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        Instant version = Instant.parse("2026-11-22T00:00:01Z");
        Instant now = Instant.parse("2026-11-22T00:00:00Z");
        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(now);
            assertThat(
                            repository.updatePendingState(
                                    "id", "NEEDS_CONSIDERATION", null, null, version))
                    .isTrue();
        }
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class), update.capture(), eq(QuestionDocument.class));
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("status", "updatedAt");
        assertThat(fields)
                .containsEntry("status", "NEEDS_CONSIDERATION")
                .containsEntry("updatedAt", version.plusMillis(1));
    }

    @Test
    @DisplayName("trả về entity đã ánh xạ khi MongoDB tìm thấy câu theo ID")
    void test_findById_006() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);
        QuestionDocument document = QuestionDocument.builder().id("id").build();
        QuestionEntity entity = QuestionEntity.builder().id("id").build();
        when(mongo.findById("id", QuestionDocument.class)).thenReturn(document);
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.findById("id")).isSameAs(entity);
    }

    @Test
    @DisplayName("trả về NotFoundException khi MongoDB không có câu theo ID")
    void test_findById_007() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);

        assertThatThrownBy(() -> repository.findById("missing"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về IDs đã cấp khi insert hàng loạt với thời điểm tạo và cập nhật được gán")
    void test_saveQuestions_008() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);
        QuestionEntity question = QuestionEntity.builder().status("APPROVED").build();
        QuestionDocument document = QuestionDocument.builder().status("APPROVED").build();
        when(mapper.toDocument(question)).thenReturn(document);
        when(mongo.insertAll(List.of(document)))
                .thenReturn(List.of(QuestionDocument.builder().id("saved-id").build()));
        Instant now = Instant.parse("2026-11-22T00:00:00Z");

        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(now);
            assertThat(repository.saveQuestions(List.of(question))).containsExactly("saved-id");
        }

        verify(mongo).insertAll(List.of(document));
        assertThat(document.getCreatedAt()).isEqualTo(now);
        assertThat(document.getUpdatedAt()).isEqualTo(now);
        assertThat(document.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("trả về entity đã ánh xạ khi truy vấn theo lessonId")
    void test_findByLessonIds_009() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);
        QuestionDocument document = QuestionDocument.builder().lessonId("lesson").build();
        QuestionEntity entity = QuestionEntity.builder().lessonId("lesson").build();
        when(mongo.find(any(Query.class), eq(QuestionDocument.class)))
                .thenReturn(List.of(document));
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.findByLessonIds(List.of("lesson"))).containsExactly(entity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(QuestionDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("lessonId", new Document("$in", List.of("lesson")));
    }

    @Test
    @DisplayName("trả về NotFoundException khi không có câu hỏi cho các lessonId")
    void test_findByLessonIds_010() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());

        assertThatThrownBy(() -> repository.findByLessonIds(List.of("missing")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("trả về entity đã ánh xạ khi truy vấn theo danh sách ID")
    void test_findByIds_011() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionMapper mapper = mock(QuestionMapper.class);
        QuestionRepositoryImpl repository = new QuestionRepositoryImpl(mongo, mapper);
        QuestionDocument document = QuestionDocument.builder().id("id").build();
        QuestionEntity entity = QuestionEntity.builder().id("id").build();
        when(mongo.find(any(Query.class), eq(QuestionDocument.class)))
                .thenReturn(List.of(document));
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repository.findByIds(List.of("id"))).containsExactly(entity);
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(QuestionDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", new Document("$in", List.of("id")));
    }

    @Test
    @DisplayName("trả về NotFoundException khi truy vấn danh sách ID không có kết quả")
    void test_findByIds_012() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        QuestionRepositoryImpl repository =
                new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());

        assertThatThrownBy(() -> repository.findByIds(List.of("missing")))
                .isInstanceOf(NotFoundException.class);
    }
}
