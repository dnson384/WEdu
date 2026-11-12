package com.wedu.exam_creation.exam.usecase;

import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnprocessableEntityException;
import com.wedu.exam_creation.exam.domain.entity.*;
import com.wedu.exam_creation.exam.domain.repository.IExamRepository;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExamMatrixUsecaseTest {
    private final IExamRepository repo = mock(IExamRepository.class);
    // Matrix reads must not need question, draft, chapter or S3 services.
    private final ExamUsecase usecase = new ExamUsecase(repo, null, null, null, null, null, null);

    @Test
    void exportsSnapshotInStoredOrderIncludingZeroWithoutReadingQuestions() {
        var lesson = new LessonExamEntity("l", "Bài tiếng Việt",
                List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 0),
                        new ExamMatrixItemEntity("Nhiều lựa chọn", "Nhận biết", 3)),
                List.of(new ExamMatrixDetailItemEntity("Dạng bài", "Thông hiểu", "Yêu cầu cần đạt", "Đúng sai", 0)));
        ExamEntity exam = new ExamEntity();
        exam.setName("Đề kiểm tra");
        exam.setChapters(List.of(new ChapterExamEntity(List.of(lesson), "c", "Chương 1")));
        when(repo.getExamById("owner", "exam")).thenReturn(exam);

        var result = usecase.getExamMatrixById("owner", "exam");

        assertThat(result.getExamName()).isEqualTo("Đề kiểm tra");
        assertThat(result.getMatrix()).extracting("selectedCount").containsExactly(0, 3);
        assertThat(result.getMatrix().get(0).getChapterName()).isEqualTo("Chương 1");
        assertThat(result.getMatrix().get(0).getLessonName()).isEqualTo("Bài tiếng Việt");
        assertThat(result.getMatrixDetails().get(0).getLearningOutcome()).isEqualTo("Yêu cầu cần đạt");
        assertThat(result.getMatrixDetails().get(0).getExerciseType()).isEqualTo("Dạng bài");
        verify(repo).getExamById("owner", "exam");
        verifyNoMoreInteractions(repo);
    }

    @Test
    void propagatesNotFoundFromOwnerFilteredRead() {
        when(repo.getExamById("other", "exam")).thenThrow(new NotFoundException("Không tồn tại bài kiểm tra"));
        assertThatThrownBy(() -> usecase.getExamMatrixById("other", "exam")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsMissingSnapshot() {
        when(repo.getExamById("owner", "exam")).thenReturn(new ExamEntity());
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam"))
                .isInstanceOf(UnprocessableEntityException.class);
    }

    @Test
    void rejectsPartiallyMissingDetails() {
        var lesson = new LessonExamEntity("l", "Bài", List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 1)), null);
        ExamEntity exam = new ExamEntity();
        exam.setChapters(List.of(new ChapterExamEntity(List.of(lesson), "c", "Chương")));
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam"))
                .isInstanceOf(UnprocessableEntityException.class);
    }
}
