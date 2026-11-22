package com.wedu.exam_creation.questionReview.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.questionReview.domain.entity.*;
import com.wedu.exam_creation.questionReview.domain.repository.IQuestionReviewRepository;
import com.wedu.exam_creation.questionReview.infrastructure.mapper.QuestionReviewMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.MockedStatic;

class QuestionReviewUsecaseTest {
    private final IQuestionReviewRepository repo = mock(IQuestionReviewRepository.class);
    private static final Instant NOW = Instant.parse("2026-11-22T00:00:00Z");
    private final QuestionReviewMapper mapper = mock(QuestionReviewMapper.class);
    private final QuestionReviewUsecase usecase = new QuestionReviewUsecase(repo, mapper);

    @Test
    @DisplayName("trả về không có ngoại lệ và tăng lượt một lần khi thêm bản sinh lại")
    void test_saveCandidate_001() {
        QuestionReviewEntity review = QuestionReviewEntity.builder().id("id").build();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        when(repo.update(any(), eq(0L))).thenReturn(true);
        QuestionDTO candidate = new QuestionDTO();
        RegeneratedQuestionEntity mapped = RegeneratedQuestionEntity.builder().build();
        when(mapper.toSnapshot(candidate)).thenReturn(mapped);

        saveAtFixedTime(candidate, 0, true);

        ArgumentCaptor<QuestionReviewEntity> captor =
                ArgumentCaptor.forClass(QuestionReviewEntity.class);
        verify(repo).update(captor.capture(), eq(0L));
        assertThat(captor.getValue().getRegenerationCount()).isEqualTo(1);
        RegeneratedQuestionEntity snapshot = captor.getValue().getRegeneratedQuestions().get(0);
        assertThat(snapshot).isSameAs(mapped);
        assertThat(snapshot.getId()).isEqualTo("id");
        assertThat(snapshot.getStatus()).isEqualTo("PENDING_REVIEW");
        assertThat(snapshot.getCreatedAt()).isEqualTo(NOW);
        assertThat(snapshot.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và giữ lượt cùng lịch sử khi sửa bản sinh lại mới nhất")
    void test_saveCandidate_002() {
        RegeneratedQuestionEntity first = RegeneratedQuestionEntity.builder().id("earlier").build();
        RegeneratedQuestionEntity latest = new RegeneratedQuestionEntity();
        latest.setCreatedAt(Instant.parse("2026-10-30T00:00:00Z"));
        QuestionReviewEntity review =
                QuestionReviewEntity.builder()
                        .id("id")
                        .revision(2)
                        .regenerationCount(2)
                        .regeneratedQuestions(new ArrayList<>(List.of(first, latest)))
                        .build();
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        when(repo.update(any(), eq(2L))).thenReturn(true);
        QuestionDTO edited = new QuestionDTO();
        edited.getQuestion().setTemplate("Đã sửa");
        RegeneratedQuestionEntity mapped =
                RegeneratedQuestionEntity.builder()
                        .question(ContentRegeneratedEntity.builder().template("Đã sửa").build())
                        .build();
        when(mapper.toSnapshot(edited)).thenReturn(mapped);

        saveAtFixedTime(edited, 2, false);

        verify(repo).update(review, 2);
        assertThat(review.getRegenerationCount()).isEqualTo(2);
        assertThat(review.getRegeneratedQuestions()).hasSize(2);
        assertThat(review.getRegeneratedQuestions().get(0)).isSameAs(first);
        assertThat(review.getRegeneratedQuestions().get(1).getCreatedAt())
                .isEqualTo(latest.getCreatedAt());
        assertThat(review.getRegeneratedQuestions().get(1).getQuestion().getTemplate())
                .isEqualTo("Đã sửa");
        assertThat(mapped.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("trả về trạng thái mặc định khi review legacy chưa có bản sinh lại")
    void test_states_003() {
        when(repo.findByIds(List.of("id")))
                .thenReturn(List.of(QuestionReviewEntity.builder().id("id").build()));
        QuestionReviewStateDTO state = usecase.states(List.of("id")).get("id");
        assertThat(state.getRegenerationCount()).isZero();
        assertThat(state.getRevision()).isZero();
        assertThat(state.getLatestQuestion()).isNull();
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ConflictException khi đã sinh lại bốn lần hoặc revision gửi lên đã cũ")
    void test_saveCandidate_004() {
        QuestionReviewEntity review =
                QuestionReviewEntity.builder().id("id").revision(4).regenerationCount(4).build();
        QuestionDTO candidate = new QuestionDTO();
        when(mapper.toSnapshot(candidate)).thenReturn(new RegeneratedQuestionEntity());
        when(repo.findByIds(List.of("id"))).thenReturn(List.of(review));
        assertThatThrownBy(() -> saveAtFixedTime(candidate, 4, true))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> saveAtFixedTime(candidate, 3, true))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về ConflictException khi cập nhật có điều kiện không khớp review")
    void test_saveCandidate_005() {
        QuestionDTO candidate = new QuestionDTO();
        when(mapper.toSnapshot(candidate)).thenReturn(new RegeneratedQuestionEntity());
        when(repo.findByIds(List.of("id")))
                .thenReturn(List.of(QuestionReviewEntity.builder().id("id").build()));

        assertThatThrownBy(() -> saveAtFixedTime(candidate, 0, true))
                .isInstanceOf(ConflictException.class);
        verify(repo).update(any(), eq(0L));
    }

    @Test
    @DisplayName("trả về ConflictException khi thao tác xóa không khớp revision")
    void test_delete_006() {
        assertThatThrownBy(() -> usecase.delete("id", 0)).isInstanceOf(ConflictException.class);
        InOrder order = inOrder(repo);
        order.verify(repo).ensureExists("id");
        order.verify(repo).delete("id", 0);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi tạo review rỗng cho các ID câu hỏi đã lưu")
    void test_createForQuestions_007() {
        usecase.createForQuestions(List.of("a", "b"));

        verify(repo)
                .saveReviews(
                        List.of(
                                QuestionReviewEntity.builder().id("a").build(),
                                QuestionReviewEntity.builder().id("b").build()));
    }

    @Test
    @DisplayName("trả về revision, số lượt và DTO mới nhất khi đọc review có nhiều bản sinh lại")
    void test_states_008() {
        RegeneratedQuestionEntity first = RegeneratedQuestionEntity.builder().id("earlier").build();
        RegeneratedQuestionEntity latest = RegeneratedQuestionEntity.builder().id("latest").build();
        QuestionReviewEntity review =
                QuestionReviewEntity.builder()
                        .id("id")
                        .revision(3)
                        .regenerationCount(2)
                        .regeneratedQuestions(List.of(first, latest))
                        .build();
        QuestionDTO candidate = new QuestionDTO();
        when(repo.findByIds(List.of("id", "missing"))).thenReturn(List.of(review));
        when(mapper.toDTO(latest)).thenReturn(candidate);

        assertThat(usecase.states(List.of("id", "missing")))
                .containsExactlyEntriesOf(
                        Map.of("id", new QuestionReviewStateDTO(2, 3, candidate)));
        verify(mapper, never()).toDTO(first);
    }

    @Test
    @DisplayName("trả về ConflictException khi yêu cầu sửa bản sinh lại nhưng lịch sử trống")
    void test_saveCandidate_009() {
        QuestionDTO candidate = new QuestionDTO();
        when(repo.findByIds(List.of("id")))
                .thenReturn(List.of(QuestionReviewEntity.builder().id("id").build()));
        when(mapper.toSnapshot(candidate)).thenReturn(new RegeneratedQuestionEntity());

        assertThatThrownBy(() -> saveAtFixedTime(candidate, 0, false))
                .isInstanceOf(ConflictException.class);
        verify(repo, never()).update(any(), anyLong());
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi xóa review khớp revision")
    void test_delete_010() {
        when(repo.delete("id", 2)).thenReturn(true);

        usecase.delete("id", 2);

        InOrder order = inOrder(repo);
        order.verify(repo).ensureExists("id");
        order.verify(repo).delete("id", 2);
    }

    private void saveAtFixedTime(QuestionDTO candidate, long revision, boolean append) {
        try (MockedStatic<Instant> clock = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
            clock.when(Instant::now).thenReturn(NOW);
            usecase.saveCandidate("id", candidate, revision, append);
        }
    }
}
