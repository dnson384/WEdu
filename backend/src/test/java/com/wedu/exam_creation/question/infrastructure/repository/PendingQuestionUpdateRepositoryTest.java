package com.wedu.exam_creation.question.infrastructure.repository;

import com.mongodb.client.result.UpdateResult;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.VariablesEntity;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PendingQuestionUpdateRepositoryTest {
    @Test
    void updatesOnlyContentAndVersionUnderPendingAndVersionGuards() {
        var mongo = mock(MongoTemplate.class);
        var repository = new QuestionRepositoryImpl(mongo, Mappers.getMapper(QuestionMapper.class));
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class))).thenReturn(UpdateResult.acknowledged(1, 1L, null));
        var version = Instant.parse("2026-10-29T00:00:00Z");
        var content = new ContentEntity("<img_0>", new VariablesEntity(Map.of(), Map.of("img_0", "media/new.png")));
        assertThat(repository.updatePendingContent("id", content, List.of(), version)).isTrue();
        var query = ArgumentCaptor.forClass(Query.class);
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(query.capture(), update.capture(), eq(QuestionDocument.class));
        assertThat(query.getValue().getQueryObject()).containsEntry("_id", "id").containsEntry("status", "PENDING_REVIEW").containsEntry("updatedAt", version);
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("question", "options", "updatedAt");
    }

    @Test
    void noMatchedPendingVersionReturnsFalse() {
        var mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class))).thenReturn(UpdateResult.acknowledged(0, 0L, null));
        var repository = new QuestionRepositoryImpl(mongo, Mappers.getMapper(QuestionMapper.class));
        assertThat(repository.updatePendingContent("id", new ContentEntity(), List.of(), null)).isFalse();
    }

    @Test
    void finalStatusOnlyChangesStatusAndTimestampUnderVersionGuard() {
        var mongo = mock(MongoTemplate.class);
        when(mongo.updateFirst(any(Query.class), any(Update.class), eq(QuestionDocument.class)))
                .thenReturn(UpdateResult.acknowledged(1, 1L, null));
        var repository = new QuestionRepositoryImpl(mongo, Mappers.getMapper(QuestionMapper.class));
        var version = Instant.now().plusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
        assertThat(repository.updatePendingState("id", "NEEDS_CONSIDERATION", null, null, version)).isTrue();
        var update = ArgumentCaptor.forClass(Update.class);
        verify(mongo).updateFirst(any(Query.class), update.capture(), eq(QuestionDocument.class));
        Document fields = update.getValue().getUpdateObject().get("$set", Document.class);
        assertThat(fields.keySet()).containsExactlyInAnyOrder("status", "updatedAt");
        assertThat(fields).containsEntry("status", "NEEDS_CONSIDERATION").containsEntry("updatedAt", version.plusMillis(1));
    }
}
