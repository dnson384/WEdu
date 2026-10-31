package com.wedu.exam_creation.question.usecase;

import com.wedu.exam_creation.ai.service.AIQuestionGenerationService;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import java.time.Instant;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class QuestionServicePendingQuestionsTest {
    private final IQuestionRepository repository = mock(IQuestionRepository.class);
    private final QuestionDTOMapper mapper = mock(QuestionDTOMapper.class);
    private final QuestionService service = new QuestionService(repository, mapper,
            mock(AIQuestionGenerationService.class), mock(TelegramNotificationService.class), mock(QuestionReviewService.class));

    @Test
    void approvedQuestionCannotBeEdited() {
        when(repository.findById("id")).thenReturn(QuestionEntity.builder().status("APPROVED").build());
        assertThatThrownBy(() -> service.getPendingForEdit("id")).isInstanceOf(ConflictException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    void conditionalUpdateFailureBecomesConflict() {
        var version = Instant.parse("2026-10-29T00:00:00Z");
        var content = new ContentDTO();
        assertThatThrownBy(() -> service.updatePendingContent("id", content, List.of(), version)).isInstanceOf(ConflictException.class);
        verify(repository).updatePendingContent(eq("id"), isNull(), eq(List.of()), eq(version));
    }

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

    @Test
    void candidateEditTouchesVersionAndSavesOnlyCandidateContent() {
        var reviews = mock(QuestionReviewService.class);
        var realMapper = org.mapstruct.factory.Mappers.getMapper(QuestionDTOMapper.class);
        var subject = new QuestionService(repository, realMapper, mock(AIQuestionGenerationService.class),
                mock(TelegramNotificationService.class), reviews);
        var version = Instant.parse("2026-10-31T00:00:00Z");
        var original = QuestionEntity.builder().id("id").status("PENDING_REVIEW").updatedAt(version).build();
        var candidate = new com.wedu.exam_creation.common.dto.question.response.QuestionDTO();
        candidate.setId("id");
        when(repository.findById("id")).thenReturn(original);
        when(reviews.state("id")).thenReturn(new com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO(2, 3, candidate));
        when(repository.updatePendingState("id", "PENDING_REVIEW", null, null, version)).thenReturn(true);
        var content = new ContentDTO();
        content.setTemplate("Đã sửa bản mới nhất");
        subject.updatePendingContent("id", content, List.of(), version, 3);
        verify(repository).updatePendingState("id", "PENDING_REVIEW", null, null, version);
        verify(repository, never()).updatePendingContent(any(), any(), any(), any());
        verify(reviews).saveCandidate("id", candidate, 3, false);
        assertThat(candidate.getQuestion().getTemplate()).isEqualTo("Đã sửa bản mới nhất");
    }

    @Test
    void failedQuestionVersionGuardDoesNotWriteCandidate() {
        var reviews = mock(QuestionReviewService.class);
        var realMapper = org.mapstruct.factory.Mappers.getMapper(QuestionDTOMapper.class);
        var subject = new QuestionService(repository, realMapper, mock(AIQuestionGenerationService.class),
                mock(TelegramNotificationService.class), reviews);
        var version = Instant.parse("2026-10-31T00:00:00Z");
        when(repository.findById("id")).thenReturn(QuestionEntity.builder().id("id").status("PENDING_REVIEW").updatedAt(version).build());
        when(reviews.state("id")).thenReturn(new com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO(1, 1,
                new com.wedu.exam_creation.common.dto.question.response.QuestionDTO()));
        assertThatThrownBy(() -> subject.updatePendingContent("id", new ContentDTO(), List.of(), version, 1))
                .isInstanceOf(ConflictException.class);
        verify(reviews, never()).saveCandidate(any(), any(), anyLong(), anyBoolean());
    }
}
