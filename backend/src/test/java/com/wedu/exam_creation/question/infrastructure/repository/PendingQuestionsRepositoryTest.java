package com.wedu.exam_creation.question.infrastructure.repository;

import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import com.wedu.exam_creation.question.infrastructure.mapper.QuestionMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PendingQuestionsRepositoryTest {
    @Test
    void filtersBeforeCountingAndUsesTenItemsWithStableDescendingSort() {
        var mongo = mock(MongoTemplate.class);
        var repository = new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        when(mongo.count(any(Query.class), eq(QuestionDocument.class))).thenReturn(19L);
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());
        var result = repository.findPending(1, "a.*", "Đúng sai", List.of("chapter"), List.of("lesson"));
        assertThat(result).isEmpty();
        assertThat(repository.countPending("a.*", "Đúng sai", List.of("chapter"), List.of("lesson"))).isEqualTo(19);
        var count = ArgumentCaptor.forClass(Query.class);
        var find = ArgumentCaptor.forClass(Query.class);
        verify(mongo).count(count.capture(), eq(QuestionDocument.class));
        verify(mongo).find(find.capture(), eq(QuestionDocument.class));
        assertThat(find.getValue().getQueryObject().toJson()).isEqualTo(count.getValue().getQueryObject().toJson());
        assertThat(find.getValue().getQueryObject().toJson()).contains("PENDING_REVIEW", "Đúng sai", "chapterId", "lessonId", "question.template", "\\\\Qa.*\\\\E");
        assertThat(count.getValue().getLimit()).isZero();
        assertThat(count.getValue().getSkip()).isZero();
        assertThat(find.getValue().getLimit()).isEqualTo(10);
        assertThat(find.getValue().getSkip()).isEqualTo(10);
        assertThat(find.getValue().getSortObject()).containsEntry("createdAt", -1).containsEntry("_id", -1);
    }

    @Test
    void emptyQueueIsAValidPageWithoutNotFound() {
        var mongo = mock(MongoTemplate.class);
        when(mongo.find(any(Query.class), eq(QuestionDocument.class))).thenReturn(List.of());
        var repository = new QuestionRepositoryImpl(mongo, mock(QuestionMapper.class));
        assertThat(repository.findPending(0, "", "", List.of(), List.of())).isEmpty();
        assertThat(repository.countPending("", "", List.of(), List.of())).isZero();
    }
}
