package com.wedu.exam_creation.question.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.ai.usecase.AIQuestionGenerationService;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.wedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionServicePlusAccessTest {
    private final IQuestionRepository repository = mock(IQuestionRepository.class);
    private final AIQuestionGenerationService ai = mock(AIQuestionGenerationService.class);
    private final UserService users = mock(UserService.class);
    private final QuestionService service =
            new QuestionService(
                    repository,
                    mock(QuestionDTOMapper.class),
                    ai,
                    mock(TelegramNotificationService.class),
                    mock(QuestionReviewService.class));
    private final List<ChapterResponseDTO> chapters =
            List.of(
                    new ChapterResponseDTO(
                            "chapter",
                            "subject",
                            "Chapter",
                            List.of(new LessonDataResponseDTO("lesson", "Lesson", List.of()))));
    private final List<ExamMatrixDetailDTO> matrix =
            List.of(
                    new ExamMatrixDetailDTO(
                            "chapter", "lesson", "exercise", "difficulty", "outcome", "type", 1));

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(service, "users", users);
        when(repository.findByLessonIds(List.of("lesson"))).thenReturn(List.of());
    }

    @ParameterizedTest
    @CsvSource({"FREE,true", "PLUS,false"})
    void stalePlusPrincipalCannotStartAi(String currentAccountType, boolean active) {
        CommonUserResponseAllDTO current = new CommonUserResponseAllDTO();
        current.setAccountType(currentAccountType);
        current.setIsActive(active);
        when(users.findById("teacher")).thenReturn(current);

        assertThatThrownBy(() -> service.generateExamQuestions("PLUS", chapters, matrix, "teacher"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(ai);
    }

    @Test
    void activePlusCanReachAiWithoutRefundAccounts() throws Exception {
        CommonUserResponseAllDTO current = new CommonUserResponseAllDTO();
        current.setAccountType("PLUS");
        current.setIsActive(true);
        when(users.findById("teacher")).thenReturn(current);
        IllegalStateException providerFailure =
                new IllegalStateException("test provider unavailable");
        when(ai.generateQuestions(anyList())).thenThrow(providerFailure);

        assertThatThrownBy(() -> service.generateExamQuestions("PLUS", chapters, matrix, "teacher"))
                .isSameAs(providerFailure);
        verify(users).findById("teacher");
        verify(ai).generateQuestions(anyList());
    }

    @Test
    void freeGenerationDoesNotStartAiOrReadPlusGate() throws Exception {
        assertThat(service.generateExamQuestions("FREE", chapters, matrix, "teacher")).isNotNull();
        verifyNoInteractions(ai, users);
    }
}
