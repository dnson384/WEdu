package com.wedu.exam_creation.question.usecase;

import com.wedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class QuestionServicePendingQuestionsTest {
    private final IQuestionRepository repository = mock(IQuestionRepository.class);
    private final QuestionDTOMapper mapper = mock(QuestionDTOMapper.class);
    private final QuestionService service = new QuestionService(repository, mapper,
            mock(AIQuestionGenerationService.class), mock(TelegramNotificationService.class), mock(QuestionReviewService.class));

    @Test
    void mapsEntitiesAndBuildsPaginationInUsecase() {
        var entity = QuestionEntity.builder().id("question-id").build();
        var dto = new PendingQuestionDTO();
        dto.setId("question-id");
        when(repository.countPending("search", "Đúng sai", List.of("chapter"), List.of("lesson"))).thenReturn(19L);
        when(repository.findPending(1, "search", "Đúng sai", List.of("chapter"), List.of("lesson"))).thenReturn(List.of(entity));
        when(mapper.entityToPendingDTO(entity)).thenReturn(dto);

        var result = service.findPending(1, "search", "Đúng sai", List.of("chapter"), List.of("lesson"));

        assertThat(result.getItems()).containsExactly(dto);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(19);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(mapper).entityToPendingDTO(entity);
    }

    @Test
    void preservesTotalWhenRequestedPageIsEmpty() {
        when(repository.countPending("", "", List.of(), List.of())).thenReturn(9L);
        when(repository.findPending(1, "", "", List.of(), List.of())).thenReturn(List.of());

        var result = service.findPending(1, "", "", List.of(), List.of());

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(1);
        verifyNoInteractions(mapper);
    }
}
