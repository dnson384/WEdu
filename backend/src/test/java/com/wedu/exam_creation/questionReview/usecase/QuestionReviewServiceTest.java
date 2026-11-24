package com.wedu.exam_creation.questionReview.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class QuestionReviewServiceTest {
    private final QuestionReviewUsecase usecase = mock(QuestionReviewUsecase.class);
    private final QuestionReviewService service = new QuestionReviewService(usecase);

    @Test
    @DisplayName("trả về không có ngoại lệ khi chuyển danh sách ID để tạo review")
    void test_createForQuestions_001() {
        service.createForQuestions(List.of("a", "b"));

        verify(usecase).createForQuestions(List.of("a", "b"));
    }

    @Test
    @DisplayName("trả về bản đồ trạng thái do use case cung cấp khi đọc hàng loạt")
    void test_states_002() {
        Map<String, QuestionReviewStateDTO> result =
                Map.of("id", new QuestionReviewStateDTO(2, 3, null));
        when(usecase.states(List.of("id"))).thenReturn(result);

        assertThat(service.states(List.of("id"))).isSameAs(result);
    }

    @Test
    @DisplayName("trả về trạng thái đúng ID khi review tồn tại")
    void test_state_003() {
        QuestionReviewStateDTO state = new QuestionReviewStateDTO(1, 2, new QuestionDTO());
        when(usecase.states(List.of("id"))).thenReturn(Map.of("id", state));

        assertThat(service.state("id")).isSameAs(state);
    }

    @Test
    @DisplayName("trả về lượt và revision bằng 0 khi câu hỏi chưa có review")
    void test_state_004() {
        when(usecase.states(List.of("id"))).thenReturn(Map.of());

        assertThat(service.state("id")).isEqualTo(new QuestionReviewStateDTO(0, 0, null));
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ khi chuyển đúng snapshot, revision và cờ append sang use case")
    void test_saveCandidate_005() {
        QuestionDTO candidate = new QuestionDTO();

        service.saveCandidate("id", candidate, 3, true);

        verify(usecase).saveCandidate("id", candidate, 3, true);
    }

    @Test
    @DisplayName("trả về ConflictException gốc khi use case từ chối xóa review theo revision cũ")
    void test_delete_006() {
        ConflictException failure = new ConflictException("Review đã thay đổi");
        doThrow(failure).when(usecase).delete("id", 3);

        assertThatThrownBy(() -> service.delete("id", 3)).isSameAs(failure);
    }
}
