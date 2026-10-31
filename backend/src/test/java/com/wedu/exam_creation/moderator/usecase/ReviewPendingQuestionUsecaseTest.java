package com.wedu.exam_creation.moderator.usecase;

import com.wedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.exception.*;
import com.wedu.exam_creation.moderator.dto.ReviewPendingQuestionDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReviewPendingQuestionUsecaseTest {
    private static final String ID = "507f1f77bcf86cd799439011";
    private final QuestionService questions = mock(QuestionService.class);
    private final QuestionReviewService reviews = mock(QuestionReviewService.class);
    private final ChapterService chapters = mock(ChapterService.class);
    private final AIQuestionGenerationService ai = mock(AIQuestionGenerationService.class);
    private final TestTransactions transactions = new TestTransactions();
    private final ReviewPendingQuestionUsecase usecase = new ReviewPendingQuestionUsecase(questions, reviews, chapters, ai, transactions);
    private PendingQuestionDTO current;
    private ReviewPendingQuestionDTO input;

    @BeforeEach
    void setup() throws Exception {
        current = new PendingQuestionDTO();
        current.setId(ID);
        current.setChapterId("chapter");
        current.setLessonId("lesson");
        current.setExerciseType("Dạng bài");
        current.setQuestionType("Trả lời ngắn");
        current.setDifficultyLevel("Thông hiểu");
        current.setLearningOutcomes(List.of("Yêu cầu 1", "Yêu cầu 2"));
        current.setUpdatedAt(Instant.parse("2026-10-31T00:00:00Z"));
        current.setQuestion(content("Câu gốc"));
        current.setOptions(List.of());
        input = new ReviewPendingQuestionDTO();
        input.setUpdatedAt(current.getUpdatedAt());
        input.setReviewRevision(0L);
        when(questions.getPendingForEdit(ID)).thenReturn(current);
        when(chapters.getAll()).thenReturn(List.of(new ChapterResponseDTO("chapter", "Vật lý", "Chương",
                List.of(LessonDataResponseDTO.builder().id("lesson").name("Bài").build()))));
        when(ai.generateQuestions(anyList())).thenReturn(List.of(generated()));
    }

    @Test
    void approvesOriginalAndDeletesQueueInOneTransaction() {
        usecase.approve(ID, input);
        verify(questions).commitPendingReview(ID, "APPROVED", null, input.getUpdatedAt());
        verify(reviews).delete(ID, 0);
        verifyNoInteractions(ai);
        assertThat(transactions.commits).isEqualTo(1);
    }

    @Test
    void approvesLatestCandidateWithoutChangingQuestionId() {
        var candidate = new QuestionDTO();
        candidate.setId(ID);
        current.setLatestRegeneratedQuestion(candidate);
        usecase.approve(ID, input);
        verify(questions).commitPendingReview(ID, "APPROVED", candidate, input.getUpdatedAt());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void rejectionGeneratesUpToFourCandidatesAndPreservesOriginal(int count) throws Exception {
        current.setRegenerationCount(count);
        var result = usecase.reject(ID, input);
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(result.getRegenerationCount()).isEqualTo(count + 1);
        var request = ArgumentCaptor.forClass(List.class);
        verify(ai).generateQuestions(request.capture());
        var specification = (GenerateQuestionRequestDTO) request.getValue().get(0);
        assertThat(specification.getNumberOfQuestions()).isEqualTo(1);
        assertThat(specification.getLearningOutcome()).isEqualTo("Yêu cầu 1; Yêu cầu 2");
        var candidate = ArgumentCaptor.forClass(QuestionDTO.class);
        verify(reviews).saveCandidate(eq(ID), candidate.capture(), eq(0L), eq(true));
        assertThat(candidate.getValue().getId()).isEqualTo(ID);
        assertThat(candidate.getValue().getLearningOutcomes()).containsExactly("Yêu cầu 1", "Yêu cầu 2");
        verify(questions).commitPendingReview(ID, "PENDING_REVIEW", null, input.getUpdatedAt());
        assertThat(current.getQuestion().getTemplate()).isEqualTo("Câu gốc");
        assertThat(transactions.commits).isEqualTo(1);
    }

    @Test
    void fifthRejectionStopsWithoutCallingAI() {
        current.setRegenerationCount(4);
        var result = usecase.reject(ID, input);
        assertThat(result.getRegenerationCount()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo("NEEDS_CONSIDERATION");
        verify(questions).commitPendingReview(ID, "NEEDS_CONSIDERATION", null, input.getUpdatedAt());
        verify(reviews).delete(ID, 0);
        verifyNoInteractions(ai, chapters);
    }

    @Test
    void failedAIConsumesNoAttemptAndExposesNoProviderDetails() throws Exception {
        when(ai.generateQuestions(anyList())).thenThrow(new IllegalStateException("private provider details"));
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(AIGenerationException.class)
                .hasMessageNotContaining("private provider details");
        verifyNoInteractions(reviews);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
        assertThat(transactions.commits).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "extra", "metadata", "outcome", "options", "placeholder", "unused", "image-url"})
    void invalidAIConsumesNoAttempt(String kind) throws Exception {
        var candidate = generated();
        switch (kind) {
            case "metadata" -> candidate.setLessonId("another lesson");
            case "outcome" -> candidate.setLearningOutcome("wrong requirement");
            case "options" -> candidate.setOptions(List.of(content("unexpected")));
            case "placeholder" -> candidate.setQuestion(content("<math_0>"));
            case "unused" -> candidate.setQuestion(new ContentDTO("Câu", new VariablesDTO(Map.of("math_0", "x"), Map.of())));
            case "image-url" -> candidate.setQuestion(new ContentDTO("<img_0>", new VariablesDTO(Map.of(), Map.of("img_0", "https://private/image"))));
            default -> { }
        }
        when(ai.generateQuestions(anyList())).thenReturn(kind.equals("empty") ? List.of()
                : kind.equals("extra") ? List.of(candidate, candidate) : List.of(candidate));
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(AIGenerationException.class);
        verifyNoInteractions(reviews);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
    }

    @Test
    void staleRequestAndMissingQuestionDoNotCallAI() {
        input.setReviewRevision(1L);
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(ConflictException.class);
        when(questions.getPendingForEdit(ID)).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(ai, reviews);
    }

    @Test
    void concurrentEditDuringAIPreventsSavingResult() throws Exception {
        when(ai.generateQuestions(anyList())).thenAnswer(call -> {
            assertThat(transactions.commits).isZero();
            assertThat(transactions.begins).isZero();
            current.setReviewRevision(1L);
            return List.of(generated());
        });
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(ConflictException.class);
        verifyNoInteractions(reviews);
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    void queueDeletionFailureRollsBackApproval() {
        doThrow(new ConflictException("changed")).when(reviews).delete(ID, 0);
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(ConflictException.class);
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(transactions.commits).isZero();
    }

    @Test
    void mongoWriteConflictReturnsConflict() {
        doThrow(new ConcurrencyFailureException("write conflict")).when(reviews).delete(ID, 0);
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(ConflictException.class);
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    void invalidInputDoesNotReadOrWrite() {
        assertThatThrownBy(() -> usecase.approve("invalid", input)).isInstanceOf(BadRequestException.class);
        input.setReviewRevision(null);
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(questions, reviews, ai);
    }

    private AIQuestionResponseDTO generated() {
        return new AIQuestionResponseDTO("chapter", "lesson", "Dạng bài", "Thông hiểu", "Yêu cầu 1; Yêu cầu 2",
                "Trả lời ngắn", content("Bản mới"), List.of());
    }

    private static ContentDTO content(String template) {
        return new ContentDTO(template, new VariablesDTO(Map.of(), Map.of()));
    }

    private static class TestTransactions extends AbstractPlatformTransactionManager {
        int begins;
        int commits;
        int rollbacks;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { begins++; }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
