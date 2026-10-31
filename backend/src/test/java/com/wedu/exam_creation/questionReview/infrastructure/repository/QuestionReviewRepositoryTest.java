package com.wedu.exam_creation.questionReview.infrastructure.repository;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionReviewRepositoryTest {
    private final MongoTemplate mongo = mock(MongoTemplate.class);
    private final QuestionReviewRepositoryImpl repo = new QuestionReviewRepositoryImpl(mongo, Mappers.getMapper(QuestionReviewMapper.class));

    @Test
    void initialRevisionMatchesLegacyMissingFieldAndUpdatesAtomically() {
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionReviewDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        assertThat(repo.update(QuestionReviewEntity.builder().id("id").regenerationCount(1).build(), 0)).isTrue();
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(query.capture(), update.capture(), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject().toJson()).contains("revision", "$or", "null");
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields).containsEntry("regenerationCount", 1).containsEntry("revision", 1L);
        assertThat(fields).containsKey("regeneratedQuestions");
    }

    @Test
    void deleteChecksExactRevisionAndReportsMissingMatch() {
        when(mongo.remove(any(Query.class), eq(QuestionReviewDocument.class))).thenReturn(DeleteResult.acknowledged(0));
        assertThat(repo.delete("id", 3)).isFalse();
        var query = ArgumentCaptor.forClass(Query.class);
        verify(mongo).remove(query.capture(), eq(QuestionReviewDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "id").containsEntry("revision", 3L);
    }

    @Test
    void ensureOnlyInitializesMissingRecords() {
        repo.ensureExists("id");
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).upsert(any(Query.class), update.capture(), eq(QuestionReviewDocument.class));
        assertThat(update.getValue().getUpdateObject().keySet()).containsExactly("$setOnInsert");
    }
}
