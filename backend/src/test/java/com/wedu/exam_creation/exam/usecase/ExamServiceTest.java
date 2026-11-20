package com.wedu.exam_creation.exam.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.exam.response.*;
import com.wedu.exam_creation.common.exception.*;
import java.util.List;
import org.junit.jupiter.api.*;

class ExamServiceTest {
    private final ExamUsecase usecase = mock(ExamUsecase.class);
    private final ExamService service = new ExamService(usecase);

    @Test
    @DisplayName("trả về chi tiết đề khi usecase tìm thấy đúng owner và examId")
    void test_getExamById_001() {
        ExamDetailDTO result = new ExamDetailDTO("exam", "owner", "Đề", List.of());
        when(usecase.getExamById("owner", "exam")).thenReturn(result);
        assertThat(service.getExamById("owner", "exam")).isSameAs(result);
    }

    @Test
    @DisplayName("trả về NotFoundException khi đề không thuộc owner")
    void test_getExamById_002() {
        NotFoundException failure = new NotFoundException("Không tồn tại bài kiểm tra");
        when(usecase.getExamById("other", "exam")).thenThrow(failure);
        assertThatThrownBy(() -> service.getExamById("other", "exam")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về snapshot khi usecase tìm thấy ma trận của owner")
    void test_getExamMatrixById_001() {
        ExamMatrixDTO result = new ExamMatrixDTO("Đề", List.of(), List.of());
        when(usecase.getExamMatrixById("owner", "exam")).thenReturn(result);
        assertThat(service.getExamMatrixById("owner", "exam")).isSameAs(result);
    }

    @Test
    @DisplayName("trả về UnprocessableEntityException khi đề thiếu snapshot")
    void test_getExamMatrixById_002() {
        UnprocessableEntityException failure = new UnprocessableEntityException("Thiếu ma trận");
        when(usecase.getExamMatrixById("owner", "exam")).thenThrow(failure);
        assertThatThrownBy(() -> service.getExamMatrixById("owner", "exam")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về NotFoundException khi snapshot đề không thuộc owner")
    void test_getExamMatrixById_003() {
        NotFoundException failure = new NotFoundException("Không tồn tại bài kiểm tra");
        when(usecase.getExamMatrixById("other", "exam")).thenThrow(failure);
        assertThatThrownBy(() -> service.getExamMatrixById("other", "exam")).isSameAs(failure);
    }
}
