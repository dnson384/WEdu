package com.wedu.exam_creation.questionReview.infrastructure.repository;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

class QuestionReviewRepositoryImplTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final QuestionReviewMapper mapper = mock(QuestionReviewMapper.class);
    private final QuestionReviewRepositoryImpl repo =
            new QuestionReviewRepositoryImpl(mongo, mapper);

    @Test
    @DisplayName("trả về true khi update revision 0 khớp cả review legacy chưa có trường revision")
    void test_update_001() {
        QuestionReviewEntity review =
                QuestionReviewEntity.builder().id("id").regenerationCount(1).build();
        when(mapper.toDocument(review)).thenReturn(QuestionReviewDocument.builder().build());
        when(mongo.updateFirst(
                        any(Query.class), any(Update.class), eq(QuestionReviewDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(repo.update(review, 0)).isTrue();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo)
                .updateFirst(query.capture(), update.capture(), eq(QuestionReviewDocument.class));
        Document filter = query.getValue().getQueryObject();
        assertThat(filter).containsEntry("_id", "id");
        List<Document> clauses = filter.getList("$and", Document.class);
        assertThat(clauses)
                .singleElement()
                .satisfies(
                        clause ->
                                assertThat(clause.getList("$or", Document.class))
                                        .containsExactlyInAnyOrder(
                                                new Document("revision", 0L),
                                                new Document("revision", null)));
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields).containsEntry("regenerationCount", 1).containsEntry("revision", 1L);
        assertThat(fields).containsKey("regeneratedQuestions");
    }

    @Test
    @DisplayName("trả về false khi không có review khớp ID và revision để xóa")
    void test_delete_002() {
        when(mongo.remove(any(Query.class), eq(QuestionReviewDocument.class)))
                .thenReturn(DeleteResult.acknowledged(0));
        assertThat(repo.delete("id", 3)).isFalse();
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "id")
                .containsEntry("revision", 3L);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi upsert chỉ gán trạng thái mặc định cho review mới")
    void test_ensureExists_003() {
        repo.ensureExists("id");
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        ArgumentCaptor<Update> update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).upsert(query.capture(), update.capture(), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject()).isEqualTo(new Document("_id", "id"));
        Document fields = update.getValue().getUpdateObject();
        assertThat(fields).containsOnlyKeys("$setOnInsert");
        assertThat(fields.get("$setOnInsert", Document.class))
                .containsEntry("regenerationCount", 0)
                .containsEntry("revision", 0L)
                .containsEntry("regeneratedQuestions", List.of());
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi insert batch bằng các document đã ánh xạ")
    void test_saveReviews_004() {
        QuestionReviewEntity a = QuestionReviewEntity.builder().id("a").build();
        QuestionReviewEntity b = QuestionReviewEntity.builder().id("b").build();
        QuestionReviewDocument first = QuestionReviewDocument.builder().id("a").build();
        QuestionReviewDocument second = QuestionReviewDocument.builder().id("b").build();
        when(mapper.toDocument(a)).thenReturn(first);
        when(mapper.toDocument(b)).thenReturn(second);

        repo.saveReviews(List.of(a, b));

        verify(mongo).insertAll(List.of(first, second));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và bỏ qua insert khi batch review rỗng")
    void test_saveReviews_005() {
        repo.saveReviews(List.of());

        verifyNoInteractions(mapper, mongo);
    }

    @Test
    @DisplayName("trả về danh sách review đã ánh xạ khi MongoDB tìm thấy các ID yêu cầu")
    void test_findByIds_006() {
        QuestionReviewDocument document = QuestionReviewDocument.builder().id("id").build();
        QuestionReviewEntity entity = QuestionReviewEntity.builder().id("id").build();
        when(mongo.find(any(Query.class), eq(QuestionReviewDocument.class)))
                .thenReturn(List.of(document));
        when(mapper.toEntity(document)).thenReturn(entity);

        assertThat(repo.findByIds(List.of("id", "missing"))).containsExactly(entity);

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).find(query.capture(), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", new Document("$in", List.of("id", "missing")));
    }

    @Test
    @DisplayName("trả về danh sách rỗng và bỏ qua MongoDB khi danh sách ID rỗng")
    void test_findByIds_007() {
        assertThat(repo.findByIds(List.of())).isEmpty();

        verifyNoInteractions(mongo, mapper);
    }

    @Test
    @DisplayName("trả về false khi update có điều kiện không khớp revision")
    void test_update_008() {
        QuestionReviewEntity review = QuestionReviewEntity.builder().id("id").build();
        when(mapper.toDocument(review)).thenReturn(QuestionReviewDocument.builder().build());
        when(mongo.updateFirst(
                        any(Query.class), any(Update.class), eq(QuestionReviewDocument.class)))
                .thenReturn(UpdateResult.acknowledged(0, 0L, null));

        assertThat(repo.update(review, 2)).isFalse();

        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongo)
                .updateFirst(query.capture(), any(Update.class), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject())
                .containsEntry("_id", "id")
                .containsEntry("revision", 2L);
    }

    @Test
    @DisplayName("trả về true khi MongoDB xóa đúng một review")
    void test_delete_009() {
        when(mongo.remove(any(Query.class), eq(QuestionReviewDocument.class)))
                .thenReturn(DeleteResult.acknowledged(1));

        assertThat(repo.delete("id", 2)).isTrue();
    }
}
