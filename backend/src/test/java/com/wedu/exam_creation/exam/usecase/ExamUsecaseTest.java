package com.wedu.exam_creation.exam.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.draft.response.*;
import com.wedu.exam_creation.common.dto.exam.response.*;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnprocessableEntityException;
import com.wedu.exam_creation.draft.usecase.DraftService;
import com.wedu.exam_creation.exam.domain.entity.*;
import com.wedu.exam_creation.exam.domain.repository.IExamRepository;
import com.wedu.exam_creation.exam.dto.mapper.ExamCommonDraftDTOMapper;
import com.wedu.exam_creation.exam.dto.mapper.ExamResponseDTOMapper;
import com.wedu.exam_creation.exam.dto.response.*;
import com.wedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.service.S3Service;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class ExamUsecaseTest {
    private final Instant timestamp = Instant.parse("2026-11-20T00:00:00Z");
    private final IExamRepository repo = mock(IExamRepository.class);
    private final QuestionService questions = mock(QuestionService.class);
    private final DraftService drafts = mock(DraftService.class);
    private final ExamResponseDTOMapper responses = mock(ExamResponseDTOMapper.class);
    private final ExamCommonDraftDTOMapper snapshots = mock(ExamCommonDraftDTOMapper.class);
    private final ChapterService chapters = mock(ChapterService.class);
    private final S3Service storage = mock(S3Service.class);
    private final ExamUsecase usecase =
            new ExamUsecase(repo, questions, drafts, responses, snapshots, chapters, storage);

    private DraftDTO prepareGeneration() {
        LessonDraftDTO lesson =
                new LessonDraftDTO(
                        "l",
                        "Bài",
                        List.of(new MatrixItemDTO("Đúng sai", "Thông hiểu", 2)),
                        List.of(
                                new MatrixDetailItemDTO(
                                        "Dạng", "Thông hiểu", "Yêu cầu", "Đúng sai", 2),
                                new MatrixDetailItemDTO(
                                        "Bỏ qua", "Nhận biết", "Yêu cầu", "Đúng sai", 0),
                                new MatrixDetailItemDTO(
                                        "Bỏ qua", "Nhận biết", "Yêu cầu", "Đúng sai", -1)));
        DraftDTO draft =
                new DraftDTO(
                        "draft",
                        "owner",
                        "Đề",
                        2,
                        List.of("Đúng sai"),
                        List.of(new ChapterDraftDTO("c", "Chương", List.of(lesson))),
                        timestamp,
                        timestamp);
        when(drafts.getDraft("draft", "owner")).thenReturn(draft);
        when(snapshots.lessonDraftDTOToLessonExamEntity(lesson))
                .thenReturn(
                        new LessonExamEntity(
                                "l",
                                "Bài",
                                List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 2)),
                                List.of(
                                        new ExamMatrixDetailItemEntity(
                                                "Dạng", "Thông hiểu", "Yêu cầu", "Đúng sai", 2))));
        when(chapters.getByIds(List.of("c")))
                .thenReturn(List.of(new ChapterResponseDTO("c", "Vật lý", "Chương", List.of())));
        return draft;
    }

    @ParameterizedTest
    @ValueSource(strings = {"FREE", "PLUS"})
    @DisplayName("trả về ID và errors cùng snapshot đã lưu khi sinh đề có các ô được chọn")
    void test_generateExam_001(String accountType) throws Exception {
        prepareGeneration();
        List<ChapterResponseDTO> context =
                List.of(new ChapterResponseDTO("c", "Vật lý", "Chương", List.of()));
        ExamGeneratedDTO generated =
                new ExamGeneratedDTO(
                        List.of(
                                new ExamQuestionGeneratedDTO(
                                        "Đúng sai", "Thông hiểu", List.of("q1", "q2"))),
                        List.of("Thiếu câu ở ô khác"));
        when(questions.generateExamQuestions(eq(accountType), eq(context), anyList(), eq("owner")))
                .thenReturn(generated);
        when(repo.saveExam(any(ExamEntity.class))).thenReturn("exam");
        ExamGeneratedResponseDTO result;
        try (MockedStatic<Instant> clock = mockStatic(Instant.class)) {
            clock.when(Instant::now).thenReturn(timestamp);
            result = usecase.generateExam("draft", "owner", accountType);
        }
        assertThat(result).isEqualTo(new ExamGeneratedResponseDTO("exam", generated.getErrors()));
        ArgumentCaptor<ExamEntity> saved = ArgumentCaptor.forClass(ExamEntity.class);
        verify(repo).saveExam(saved.capture());
        assertThat(saved.getValue())
                .isEqualTo(
                        new ExamEntity(
                                null,
                                "owner",
                                "Đề",
                                List.of(
                                        new ChapterExamEntity(
                                                List.of(
                                                        new LessonExamEntity(
                                                                "l",
                                                                "Bài",
                                                                List.of(
                                                                        new ExamMatrixItemEntity(
                                                                                "Đúng sai",
                                                                                "Thông hiểu",
                                                                                2)),
                                                                List.of(
                                                                        new ExamMatrixDetailItemEntity(
                                                                                "Dạng",
                                                                                "Thông hiểu",
                                                                                "Yêu cầu",
                                                                                "Đúng sai",
                                                                                2)))),
                                                "c",
                                                "Chương")),
                                List.of(
                                        new QuestionExamEntity(
                                                "Đúng sai", "Thông hiểu", List.of("q1", "q2"))),
                                timestamp,
                                timestamp));
        verify(questions)
                .generateExamQuestions(
                        accountType,
                        context,
                        List.of(
                                new ExamMatrixDetailDTO(
                                        "c", "l", "Dạng", "Thông hiểu", "Yêu cầu", "Đúng sai", 2)),
                        "owner");
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về NotFoundException và không sinh hoặc lưu đề khi nháp không thuộc owner")
    void test_generateExam_002() {
        when(drafts.getDraft("draft", "other"))
                .thenThrow(new NotFoundException("Không tồn tại nháp"));
        assertThatThrownBy(() -> usecase.generateExam("draft", "other", "FREE"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(repo, questions, chapters, snapshots);
    }

    @Test
    @DisplayName("trả về InternalServerException và không lưu khi dịch vụ sinh đề trả null")
    void test_generateExam_003() {
        prepareGeneration();
        assertThatThrownBy(() -> usecase.generateExam("draft", "owner", "FREE"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Lỗi trong quá trình sinh đề");
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về lỗi dịch vụ câu hỏi và không lưu khi sinh câu hỏi thất bại")
    void test_generateExam_004() throws Exception {
        prepareGeneration();
        IllegalStateException failure = new IllegalStateException("generation failed");
        when(questions.generateExamQuestions(eq("PLUS"), anyList(), anyList(), eq("owner")))
                .thenThrow(failure);
        assertThatThrownBy(() -> usecase.generateExam("draft", "owner", "PLUS")).isSameAs(failure);
        verifyNoInteractions(repo);
    }

    @Test
    @DisplayName("trả về lỗi repository khi sinh câu hỏi thành công nhưng lưu đề thất bại")
    void test_generateExam_005() throws Exception {
        prepareGeneration();
        when(questions.generateExamQuestions(eq("FREE"), anyList(), anyList(), eq("owner")))
                .thenReturn(new ExamGeneratedDTO(List.of(), List.of()));
        IllegalStateException failure = new IllegalStateException("save failed");
        when(repo.saveExam(any(ExamEntity.class))).thenThrow(failure);
        assertThatThrownBy(() -> usecase.generateExam("draft", "owner", "FREE")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về lỗi chapter và không sinh hoặc lưu khi không đọc được context chương")
    void test_generateExam_006() {
        prepareGeneration();
        NotFoundException failure = new NotFoundException("Không tồn tại chương");
        when(chapters.getByIds(List.of("c"))).thenThrow(failure);
        assertThatThrownBy(() -> usecase.generateExam("draft", "owner", "FREE")).isSameAs(failure);
        verifyNoInteractions(questions, repo);
    }

    @Test
    @DisplayName("trả về DTO đúng thứ tự khi owner có nhiều đề")
    void test_getAllUserExams_001() {
        ExamEntity first = new ExamEntity();
        first.setId("e1");
        ExamEntity second = new ExamEntity();
        second.setId("e2");
        ExamDTO firstDTO = new ExamDTO("e1", "Đề 1", List.of(), 1);
        ExamDTO secondDTO = new ExamDTO("e2", "Đề 2", List.of(), 2);
        when(repo.getAllUserExams("owner")).thenReturn(List.of(first, second));
        when(responses.convertToExamResponse(first)).thenReturn(firstDTO);
        when(responses.convertToExamResponse(second)).thenReturn(secondDTO);
        assertThat(usecase.getAllUserExams("owner")).containsExactly(firstDTO, secondDTO);
    }

    @Test
    @DisplayName("trả về NotFoundException khi repository báo owner chưa có đề")
    void test_getAllUserExams_002() {
        NotFoundException failure = new NotFoundException("Danh sách bài kiểm tra trống");
        when(repo.getAllUserExams("owner")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.getAllUserExams("owner")).isSameAs(failure);
        verifyNoInteractions(responses);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi repository trả danh sách đề rỗng")
    void test_getAllUserExams_003() {
        when(repo.getAllUserExams("owner")).thenReturn(List.of());
        assertThat(usecase.getAllUserExams("owner")).isEmpty();
        verifyNoInteractions(responses);
    }

    @Test
    @DisplayName("trả về chi tiết được map khi đọc đúng owner và gom ID câu hỏi của mọi nhóm")
    void test_getExamById_001() {
        ExamEntity exam = new ExamEntity();
        exam.setQuestions(
                List.of(
                        new QuestionExamEntity("Đúng sai", "Nhận biết", List.of("q1", "q2")),
                        new QuestionExamEntity("Trả lời ngắn", "Thông hiểu", List.of("q3"))));
        List<QuestionDTO> found = List.of(new QuestionDTO());
        ExamDetailDTO detail = new ExamDetailDTO("exam", "owner", "Đề", List.of());
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        when(questions.findByIds(List.of("q1", "q2", "q3"))).thenReturn(found);
        when(responses.convertToExamDetailResponse(exam, found, storage)).thenReturn(detail);
        assertThat(usecase.getExamById("owner", "exam")).isSameAs(detail);
    }

    @Test
    @DisplayName("trả về NotFoundException và không đọc câu hỏi khi đề không thuộc owner")
    void test_getExamById_002() {
        when(repo.getExamById("other", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại bài kiểm tra"));
        assertThatThrownBy(() -> usecase.getExamById("other", "exam"))
                .isInstanceOf(NotFoundException.class);
        verifyNoInteractions(questions, responses, storage);
    }

    @Test
    @DisplayName("trả về lỗi câu hỏi và không map chi tiết khi không hydrate được câu hỏi")
    void test_getExamById_003() {
        ExamEntity exam = new ExamEntity();
        exam.setQuestions(List.of());
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        IllegalStateException failure = new IllegalStateException("read failed");
        when(questions.findByIds(List.of())).thenThrow(failure);
        assertThatThrownBy(() -> usecase.getExamById("owner", "exam")).isSameAs(failure);
        verifyNoInteractions(responses);
    }

    @Test
    @DisplayName("trả về danh sách gần đây đúng thứ tự repository khi owner có đề")
    void test_getRecentExams_001() {
        ExamEntity first = new ExamEntity();
        first.setId("e2");
        ExamEntity second = new ExamEntity();
        second.setId("e1");
        ExamDTO firstDTO = new ExamDTO("e2", "Mới", List.of(), 2);
        ExamDTO secondDTO = new ExamDTO("e1", "Cũ", List.of(), 1);
        when(repo.getRecentExams("owner")).thenReturn(List.of(first, second));
        when(responses.convertToExamResponse(first)).thenReturn(firstDTO);
        when(responses.convertToExamResponse(second)).thenReturn(secondDTO);
        assertThat(usecase.getRecentExams("owner")).containsExactly(firstDTO, secondDTO);
    }

    @Test
    @DisplayName("trả về danh sách rỗng khi owner chưa có đề gần đây")
    void test_getRecentExams_002() {
        when(repo.getRecentExams("owner")).thenReturn(List.of());
        assertThat(usecase.getRecentExams("owner")).isEmpty();
        verifyNoInteractions(responses);
    }

    @Test
    @DisplayName("trả về lỗi repository khi không đọc được đề gần đây")
    void test_getRecentExams_003() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(repo.getRecentExams("owner")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.getRecentExams("owner")).isSameAs(failure);
        verifyNoInteractions(responses);
    }

    @Test
    @DisplayName("trả về true và không xóa câu hỏi khi xóa được đề của owner")
    void test_deleteExam_001() {
        when(repo.deleteExam("owner", "exam")).thenReturn(true);
        assertThat(usecase.deleteExam("owner", "exam")).isTrue();
        verify(repo).deleteExam("owner", "exam");
        verifyNoInteractions(questions, storage, drafts);
    }

    @Test
    @DisplayName("trả về NotFoundException khi đề cần xóa không tồn tại hoặc không thuộc owner")
    void test_deleteExam_002() {
        assertThatThrownBy(() -> usecase.deleteExam("other", "exam"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Không tìm thấy đề kiểm tra để xóa");
        verify(repo).deleteExam("other", "exam");
    }

    @Test
    @DisplayName("trả về lỗi repository khi thao tác xóa đề thất bại")
    void test_deleteExam_003() {
        IllegalStateException failure = new IllegalStateException("delete failed");
        when(repo.deleteExam("owner", "exam")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.deleteExam("owner", "exam")).isSameAs(failure);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "chaptersEmpty",
                "lessonsNull",
                "lessonsEmpty",
                "matrixNull",
                "matrixEmpty",
                "detailsEmpty"
            })
    @DisplayName(
            "trả về UnprocessableEntityException khi snapshot không có đủ dòng ma trận và đặc tả")
    void test_getExamMatrixById_005(String missing) {
        LessonExamEntity lesson =
                new LessonExamEntity(
                        "l",
                        "Bài",
                        List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 1)),
                        List.of(
                                new ExamMatrixDetailItemEntity(
                                        "Dạng", "Thông hiểu", "Yêu cầu", "Đúng sai", 1)));
        ChapterExamEntity chapter = new ChapterExamEntity(List.of(lesson), "c", "Chương");
        ExamEntity exam = new ExamEntity();
        exam.setChapters(List.of(chapter));
        switch (missing) {
            case "chaptersEmpty" -> exam.setChapters(List.of());
            case "lessonsNull" -> chapter.setLessons(null);
            case "lessonsEmpty" -> chapter.setLessons(List.of());
            case "matrixNull" -> lesson.setMatrix(null);
            case "matrixEmpty" -> lesson.setMatrix(List.of());
            case "detailsEmpty" -> lesson.setMatrixDetails(List.of());
            default -> throw new IllegalArgumentException(missing);
        }
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam"))
                .isInstanceOf(UnprocessableEntityException.class);
        verifyNoInteractions(questions, drafts, storage);
    }

    @Test
    @DisplayName("trả về lỗi repository khi không đọc được snapshot đề")
    void test_getExamMatrixById_006() {
        IllegalStateException failure = new IllegalStateException("read failed");
        when(repo.getExamById("owner", "exam")).thenThrow(failure);
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam")).isSameAs(failure);
    }

    @Test
    @DisplayName("trả về đề không có errors và giữ đủ snapshot khi nháp có nhiều chương và bài")
    void test_generateExam_007() throws Exception {
        DraftDTO draft = prepareGeneration();
        LessonDraftDTO second = new LessonDraftDTO("l2", "Bài 2", List.of(), List.of());
        LessonDraftDTO third = new LessonDraftDTO("l3", "Bài 3", List.of(), List.of());
        LessonExamEntity secondSnapshot = new LessonExamEntity("l2", "Bài 2", List.of(), List.of());
        LessonExamEntity thirdSnapshot = new LessonExamEntity("l3", "Bài 3", List.of(), List.of());
        draft.setChapters(
                List.of(
                        draft.getChapters().get(0),
                        new ChapterDraftDTO("c2", "Chương 2", List.of(second, third))));
        when(snapshots.lessonDraftDTOToLessonExamEntity(second)).thenReturn(secondSnapshot);
        when(snapshots.lessonDraftDTOToLessonExamEntity(third)).thenReturn(thirdSnapshot);
        List<ChapterResponseDTO> context =
                List.of(
                        new ChapterResponseDTO("c", "Vật lý", "Chương", List.of()),
                        new ChapterResponseDTO("c2", "Vật lý", "Chương 2", List.of()));
        when(chapters.getByIds(List.of("c", "c2"))).thenReturn(context);
        when(questions.generateExamQuestions(eq("FREE"), eq(context), anyList(), eq("owner")))
                .thenReturn(
                        new ExamGeneratedDTO(
                                List.of(
                                        new ExamQuestionGeneratedDTO(
                                                "Đúng sai", "Thông hiểu", List.of("q1", "q2"))),
                                List.of()));
        when(repo.saveExam(any(ExamEntity.class))).thenReturn("exam");
        assertThat(usecase.generateExam("draft", "owner", "FREE"))
                .isEqualTo(new ExamGeneratedResponseDTO("exam", List.of()));
        ArgumentCaptor<ExamEntity> saved = ArgumentCaptor.forClass(ExamEntity.class);
        verify(repo).saveExam(saved.capture());
        assertThat(saved.getValue().getChapters())
                .extracting(ChapterExamEntity::getId)
                .containsExactly("c", "c2");
        assertThat(saved.getValue().getChapters().get(1))
                .isEqualTo(
                        new ChapterExamEntity(
                                List.of(secondSnapshot, thirdSnapshot), "c2", "Chương 2"));
        verify(questions)
                .generateExamQuestions(
                        "FREE",
                        context,
                        List.of(
                                new ExamMatrixDetailDTO(
                                        "c", "l", "Dạng", "Thông hiểu", "Yêu cầu", "Đúng sai", 2)),
                        "owner");
    }

    @Test
    @DisplayName(
            "trả về snapshot đủ trường theo thứ tự chương bài khi có nhiều chương và chương không có lessons")
    void test_getExamMatrixById_007() {
        LessonExamEntity first =
                new LessonExamEntity(
                        "l1",
                        "Bài 1",
                        List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 0)),
                        List.of(
                                new ExamMatrixDetailItemEntity(
                                        "Dạng 1", "Thông hiểu", "Yêu cầu 1", "Đúng sai", 0)));
        LessonExamEntity second =
                new LessonExamEntity(
                        "l2",
                        "Bài 2",
                        List.of(new ExamMatrixItemEntity("Trả lời ngắn", "Nhận biết", 2)),
                        List.of(
                                new ExamMatrixDetailItemEntity(
                                        "Dạng 2", "Nhận biết", "Yêu cầu 2", "Trả lời ngắn", 2)));
        ExamEntity exam = new ExamEntity();
        exam.setName("Đề");
        exam.setChapters(
                List.of(
                        new ChapterExamEntity(null, "skip", "Không có bài"),
                        new ChapterExamEntity(List.of(first), "c1", "Chương 1"),
                        new ChapterExamEntity(List.of(second), "c2", "Chương 2")));
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        assertThat(usecase.getExamMatrixById("owner", "exam"))
                .isEqualTo(
                        new ExamMatrixDTO(
                                "Đề",
                                List.of(
                                        new ExamMatrixDTO.MatrixRowDTO(
                                                "Chương 1", "Bài 1", "Đúng sai", "Thông hiểu", 0),
                                        new ExamMatrixDTO.MatrixRowDTO(
                                                "Chương 2",
                                                "Bài 2",
                                                "Trả lời ngắn",
                                                "Nhận biết",
                                                2)),
                                List.of(
                                        new ExamMatrixDTO.MatrixDetailRowDTO(
                                                "Chương 1",
                                                "Bài 1",
                                                "Đúng sai",
                                                "Thông hiểu",
                                                0,
                                                "Dạng 1",
                                                "Yêu cầu 1"),
                                        new ExamMatrixDTO.MatrixDetailRowDTO(
                                                "Chương 2",
                                                "Bài 2",
                                                "Trả lời ngắn",
                                                "Nhận biết",
                                                2,
                                                "Dạng 2",
                                                "Yêu cầu 2"))));
        verifyNoInteractions(questions, storage, drafts, chapters, snapshots, responses);
    }

    @Test
    @DisplayName("trả về snapshot đúng thứ tự gồm số lượng 0 khi đề có đủ ma trận và đặc tả")
    void test_getExamMatrixById_001() {
        LessonExamEntity lesson =
                new LessonExamEntity(
                        "l",
                        "Bài tiếng Việt",
                        List.of(
                                new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 0),
                                new ExamMatrixItemEntity("Nhiều lựa chọn", "Nhận biết", 3)),
                        List.of(
                                new ExamMatrixDetailItemEntity(
                                        "Dạng bài",
                                        "Thông hiểu",
                                        "Yêu cầu cần đạt",
                                        "Đúng sai",
                                        0)));
        ExamEntity exam = new ExamEntity();
        exam.setName("Đề kiểm tra");
        exam.setChapters(List.of(new ChapterExamEntity(List.of(lesson), "c", "Chương 1")));
        when(repo.getExamById("owner", "exam")).thenReturn(exam);

        ExamMatrixDTO result = usecase.getExamMatrixById("owner", "exam");

        assertThat(result.getExamName()).isEqualTo("Đề kiểm tra");
        assertThat(result.getMatrix()).extracting("selectedCount").containsExactly(0, 3);
        assertThat(result.getMatrix().get(0).getChapterName()).isEqualTo("Chương 1");
        assertThat(result.getMatrix().get(0).getLessonName()).isEqualTo("Bài tiếng Việt");
        assertThat(result.getMatrixDetails().get(0).getLearningOutcome())
                .isEqualTo("Yêu cầu cần đạt");
        assertThat(result.getMatrixDetails().get(0).getExerciseType()).isEqualTo("Dạng bài");
        verify(repo).getExamById("owner", "exam");
        verifyNoMoreInteractions(repo);
        verifyNoInteractions(questions, drafts, responses, snapshots, chapters, storage);
    }

    @Test
    @DisplayName("trả về NotFoundException khi đề không thuộc owner hoặc không tồn tại")
    void test_getExamMatrixById_002() {
        when(repo.getExamById("other", "exam"))
                .thenThrow(new NotFoundException("Không tồn tại bài kiểm tra"));
        assertThatThrownBy(() -> usecase.getExamMatrixById("other", "exam"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("trả về UnprocessableEntityException khi đề không có snapshot chương")
    void test_getExamMatrixById_003() {
        when(repo.getExamById("owner", "exam")).thenReturn(new ExamEntity());
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam"))
                .isInstanceOf(UnprocessableEntityException.class);
    }

    @Test
    @DisplayName("trả về UnprocessableEntityException khi bài có ma trận nhưng thiếu đặc tả")
    void test_getExamMatrixById_004() {
        LessonExamEntity lesson =
                new LessonExamEntity(
                        "l",
                        "Bài",
                        List.of(new ExamMatrixItemEntity("Đúng sai", "Thông hiểu", 1)),
                        null);
        ExamEntity exam = new ExamEntity();
        exam.setChapters(List.of(new ChapterExamEntity(List.of(lesson), "c", "Chương")));
        when(repo.getExamById("owner", "exam")).thenReturn(exam);
        assertThatThrownBy(() -> usecase.getExamMatrixById("owner", "exam"))
                .isInstanceOf(UnprocessableEntityException.class);
    }
}
