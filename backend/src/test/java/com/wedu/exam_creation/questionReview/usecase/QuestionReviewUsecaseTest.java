package com.wedu.exam_creation.questionReview.usecase;

import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.questionReview.domain.entity.*;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class QuestionReviewUsecaseTest {
    private final IQuestionReviewRepository repo = mock(IQuestionReviewRepository.class);
    private final QuestionReviewMapper mapper = Mappers.getMapper(QuestionReviewMapper.class);
    private final QuestionReviewUsecase usecase = new QuestionReviewUsecase(repo, mapper);

    @Test
    void appendCountsOnceAndPreservesContentMetadataAndKeys() {
        var review = QuestionReviewEntity.builder().id("id").build();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        when(repo.update(any(), eq(0L))).thenReturn(true);
        var candidate = candidate();
        usecase.saveCandidate("id", candidate, 0, true);
        var captor = ArgumentCaptor.forClass(QuestionReviewEntity.class);
        verify(repo).update(captor.capture(), eq(0L));
        assertThat(captor.getValue().getRegenerationCount()).isEqualTo(1);
        var snapshot = captor.getValue().getRegeneratedQuestions().get(0);
        assertThat(snapshot.getId()).isEqualTo("id");
        assertThat(snapshot.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(snapshot.getQuestion().getVariables().getImage()).containsEntry("img_0", "media/image.png");
        assertThat(snapshot.getCreatedAt()).isNotNull();
        var state = usecase.states(List.of("id")).get("id");
        assertThat(state.getLatestQuestion()).usingRecursiveComparison().isEqualTo(candidate);
    }

    @Test
    void editingLatestDoesNotConsumeAttemptOrChangeEarlierSnapshots() {
        var first = mapper.toSnapshot(candidate());
        var latest = mapper.toSnapshot(candidate());
        latest.setCreatedAt(Instant.parse("2026-10-30T00:00:00Z"));
        var review = QuestionReviewEntity.builder().id("id").revision(2).regenerationCount(2)
                .regeneratedQuestions(new ArrayList<>(List.of(first, latest))).build();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        when(repo.update(any(), eq(2L))).thenReturn(true);
        var edited = candidate();
        edited.getQuestion().setTemplate("Đã sửa");
        usecase.saveCandidate("id", edited, 2, false);
        assertThat(review.getRegenerationCount()).isEqualTo(2);
        assertThat(review.getRegeneratedQuestions()).hasSize(2);
        assertThat(review.getRegeneratedQuestions().get(0)).isSameAs(first);
        assertThat(review.getRegeneratedQuestions().get(1).getCreatedAt()).isEqualTo(latest.getCreatedAt());
        assertThat(review.getRegeneratedQuestions().get(1).getQuestion().getTemplate()).isEqualTo("Đã sửa");
    }

    @Test
    void legacyEmptyReviewAndMissingReviewReadWithZeroDefaults() {
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(QuestionReviewEntity.builder().id("id").build()));
        var state = usecase.states(List.of("id")).get("id");
        assertThat(state.getRegenerationCount()).isZero();
        assertThat(state.getRevision()).isZero();
        assertThat(state.getLatestQuestion()).isNull();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of());
        var service = new QuestionReviewService(usecase);
        assertThat(service.state("id")).isEqualTo(new QuestionReviewStateDTO(0, 0, null));
    }

    @Test
    void fifthGenerationAndStaleRevisionsAreRejected() {
        var review = QuestionReviewEntity.builder().id("id").revision(4).regenerationCount(4).build();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        assertThatThrownBy(() -> usecase.saveCandidate("id", candidate(), 4, true)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> usecase.saveCandidate("id", candidate(), 3, true)).isInstanceOf(ConflictException.class);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    void lostConditionalWriteOrDeleteReturnsConflict() {
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(QuestionReviewEntity.builder().id("id").build()));
        assertThatThrownBy(() -> usecase.saveCandidate("id", candidate(), 0, true)).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> usecase.delete("id", 0)).isInstanceOf(ConflictException.class);
        verify(repo, atLeastOnce()).ensureExists("id");
    }

    private QuestionDTO candidate() {
        return new QuestionDTO("id", "chapter", "lesson", "Dạng bài", "Thông hiểu", List.of("Yêu cầu"), "Trả lời ngắn",
                new ContentDTO("<math_0> <img_0>", new VariablesDTO(Map.of("math_0", "x"), Map.of("img_0", "media/image.png"))), List.of());
    }
}
