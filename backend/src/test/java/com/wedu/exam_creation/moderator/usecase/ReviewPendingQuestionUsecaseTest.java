package com.wedu.exam_creation.moderator.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.ai.usecase.AIQuestionGenerationService;
import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.common.exception.AIGenerationException;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.moderator.dto.ReviewPendingQuestionDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

class ReviewPendingQuestionUsecaseTest {
    private static final String ID = "507f1f77bcf86cd799439011";
    private final QuestionService questions = mock(QuestionService.class);
    private final QuestionReviewService reviews = mock(QuestionReviewService.class);
    private final ChapterService chapters = mock(ChapterService.class);
    private final AIQuestionGenerationService ai = mock(AIQuestionGenerationService.class);
    private final TestTransactions transactions = new TestTransactions();
    private final ReviewPendingQuestionUsecase usecase =
            new ReviewPendingQuestionUsecase(questions, reviews, chapters, ai, transactions);
    private PendingQuestionDTO current;
    private ReviewPendingQuestionDTO input;

    private static ContentDTO content(String template) {
        return new ContentDTO(template, new VariablesDTO(Map.of(), Map.of()));
    }

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
        when(chapters.getAll())
                .thenReturn(
                        List.of(
                                new ChapterResponseDTO(
                                        "chapter",
                                        "Vật lý",
                                        "Chương",
                                        List.of(
                                                LessonDataResponseDTO.builder()
                                                        .id("lesson")
                                                        .name("Bài")
                                                        .build()))));
        when(ai.generateQuestions(anyList())).thenReturn(List.of(generated()));
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi duyệt bản gốc và xóa hàng chờ trong cùng giao dịch")
    void test_approve_001() {
        usecase.approve(ID, input);
        verify(questions).commitPendingReview(ID, "APPROVED", null, input.getUpdatedAt());
        verify(reviews).delete(ID, 0);
        verifyNoInteractions(ai);
        assertThat(transactions.commits).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về AiAccessException gốc khi vượt ngân sách và chưa tính lượt sinh lại")
    void test_reject_002() throws Exception {
        com.wedu.exam_creation.common.exception.AiAccessException blocked =
                new com.wedu.exam_creation.common.exception.AiAccessException(
                        "AI_BUDGET_EXCEEDED", 429, "limit");
        when(ai.generateQuestions(anyList())).thenThrow(blocked);
        assertThatThrownBy(() -> usecase.reject(ID, input)).isSameAs(blocked);
        verifyNoInteractions(reviews);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
        assertThat(transactions.commits).isZero();
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi duyệt bản sinh lại mới nhất trên cùng ID")
    void test_approve_003() {
        QuestionDTO candidate = new QuestionDTO();
        candidate.setId(ID);
        current.setLatestRegeneratedQuestion(candidate);
        usecase.approve(ID, input);
        verify(questions).commitPendingReview(ID, "APPROVED", candidate, input.getUpdatedAt());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    @DisplayName("trả về PENDING_REVIEW và bản sinh lại khi từ chối trong bốn lượt đầu")
    void test_reject_004(int count) throws Exception {
        current.setRegenerationCount(count);
        com.wedu.exam_creation.moderator.dto.RejectPendingQuestionResponseDTO result =
                usecase.reject(ID, input);
        assertThat(result.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(result.getRegenerationCount()).isEqualTo(count + 1);
        ArgumentCaptor<List<GenerateQuestionRequestDTO>> request = ArgumentCaptor.captor();
        verify(ai).generateQuestions(request.capture());
        GenerateQuestionRequestDTO specification = request.getValue().get(0);
        assertThat(specification.getNumberOfQuestions()).isEqualTo(1);
        assertThat(specification.getLearningOutcome()).isEqualTo("Yêu cầu 1; Yêu cầu 2");
        ArgumentCaptor<QuestionDTO> candidate = ArgumentCaptor.forClass(QuestionDTO.class);
        verify(reviews).saveCandidate(eq(ID), candidate.capture(), eq(0L), eq(true));
        assertThat(candidate.getValue().getId()).isEqualTo(ID);
        assertThat(candidate.getValue().getLearningOutcomes())
                .containsExactly("Yêu cầu 1", "Yêu cầu 2");
        verify(questions).commitPendingReview(ID, "PENDING_REVIEW", null, input.getUpdatedAt());
        assertThat(current.getQuestion().getTemplate()).isEqualTo("Câu gốc");
        assertThat(transactions.commits).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về NEEDS_CONSIDERATION khi từ chối lần thứ năm và không gọi AI")
    void test_reject_005() {
        current.setRegenerationCount(4);
        com.wedu.exam_creation.moderator.dto.RejectPendingQuestionResponseDTO result =
                usecase.reject(ID, input);
        assertThat(result.getRegenerationCount()).isEqualTo(5);
        assertThat(result.getStatus()).isEqualTo("NEEDS_CONSIDERATION");
        verify(questions)
                .commitPendingReview(ID, "NEEDS_CONSIDERATION", null, input.getUpdatedAt());
        verify(reviews).delete(ID, 0);
        verifyNoInteractions(ai, chapters);
    }

    @Test
    @DisplayName("trả về AIGenerationException chung khi AI thất bại và chưa tính lượt")
    void test_reject_006() throws Exception {
        when(ai.generateQuestions(anyList()))
                .thenThrow(new IllegalStateException("private provider details"));
        assertThatThrownBy(() -> usecase.reject(ID, input))
                .isInstanceOf(AIGenerationException.class)
                .hasMessageNotContaining("private provider details");
        verifyNoInteractions(reviews);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
        assertThat(transactions.commits).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "empty",
                "extra",
                "metadata",
                "outcome",
                "options",
                "placeholder",
                "unused",
                "image-url"
            })
    @DisplayName(
            "trả về AIGenerationException khi dữ liệu sinh lại không phù hợp và chưa tính lượt")
    void test_reject_007(String kind) throws Exception {
        AIQuestionResponseDTO candidate = generated();
        switch (kind) {
            case "metadata" -> candidate.setLessonId("another lesson");
            case "outcome" -> candidate.setLearningOutcome("wrong requirement");
            case "options" -> candidate.setOptions(List.of(content("unexpected")));
            case "placeholder" -> candidate.setQuestion(content("<math_0>"));
            case "unused" ->
                    candidate.setQuestion(
                            new ContentDTO(
                                    "Câu", new VariablesDTO(Map.of("math_0", "x"), Map.of())));
            case "image-url" ->
                    candidate.setQuestion(
                            new ContentDTO(
                                    "<img_0>",
                                    new VariablesDTO(
                                            Map.of(), Map.of("img_0", "https://private/image"))));
            default -> {}
        }
        when(ai.generateQuestions(anyList()))
                .thenReturn(
                        kind.equals("empty")
                                ? List.of()
                                : kind.equals("extra")
                                        ? List.of(candidate, candidate)
                                        : List.of(candidate));
        assertThatThrownBy(() -> usecase.reject(ID, input))
                .isInstanceOf(AIGenerationException.class);
        verifyNoInteractions(reviews);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
    }

    @Test
    @DisplayName(
            "trả về ConflictException hoặc NotFoundException khi request cũ hoặc câu hỏi không còn")
    void test_reject_008() {
        input.setReviewRevision(1L);
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(ConflictException.class);
        when(questions.getPendingForEdit(ID)).thenThrow(new NotFoundException("missing"));
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(NotFoundException.class);
        verifyNoInteractions(ai, reviews);
    }

    @Test
    @DisplayName("trả về ConflictException khi câu hỏi thay đổi trong lúc AI sinh lại")
    void test_reject_009() throws Exception {
        when(ai.generateQuestions(anyList()))
                .thenAnswer(
                        call -> {
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
    @DisplayName("trả về ngoại lệ và rollback khi xóa hàng chờ sau duyệt thất bại")
    void test_approve_010() {
        doThrow(new ConflictException("changed")).when(reviews).delete(ID, 0);
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(ConflictException.class);
        assertThat(transactions.rollbacks).isEqualTo(1);
        assertThat(transactions.commits).isZero();
    }

    @Test
    @DisplayName("trả về ConflictException khi MongoDB báo xung đột ghi")
    void test_approve_011() {
        doThrow(new ConcurrencyFailureException("write conflict")).when(reviews).delete(ID, 0);
        assertThatThrownBy(() -> usecase.approve(ID, input)).isInstanceOf(ConflictException.class);
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    @DisplayName("trả về BadRequestException khi ID duyệt bị thiếu")
    void test_approve_012() {
        assertThatThrownBy(() -> usecase.approve(null, input))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(questions, reviews, ai);
    }

    @Test
    @DisplayName("trả về BadRequestException khi yêu cầu từ chối thiếu review revision")
    void test_reject_013() {
        input.setReviewRevision(null);
        assertThatThrownBy(() -> usecase.reject(ID, input)).isInstanceOf(BadRequestException.class);
        verifyNoInteractions(questions, reviews, ai);
    }

    @Test
    @DisplayName("trả về NotFoundException khi ID dạng chuỗi không khớp câu hỏi nào")
    void test_approve_014() {
        when(questions.getPendingForEdit("invalid"))
                .thenThrow(new NotFoundException("Không tìm thấy câu hỏi"));

        assertThatThrownBy(() -> usecase.approve("invalid", input))
                .isInstanceOf(NotFoundException.class);

        verifyNoInteractions(reviews, ai);
        verify(questions, never()).commitPendingReview(any(), any(), any(), any());
    }

    private AIQuestionResponseDTO generated() {
        return new AIQuestionResponseDTO(
                "chapter",
                "lesson",
                "Dạng bài",
                "Thông hiểu",
                "Yêu cầu 1; Yêu cầu 2",
                "Trả lời ngắn",
                content("Bản mới"),
                List.of());
    }

    private static class TestTransactions extends AbstractPlatformTransactionManager {
        int begins;
        int commits;
        int rollbacks;

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            begins++;
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            commits++;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            rollbacks++;
        }
    }
}
