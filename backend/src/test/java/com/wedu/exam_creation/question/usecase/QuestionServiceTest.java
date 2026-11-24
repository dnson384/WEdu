package com.wedu.exam_creation.question.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.ai.usecase.AIQuestionGenerationService;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamGeneratedDTO;
import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionPageDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionReviewStateDTO;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.ConflictException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.notification.telegram.TelegramNotificationService;
import com.wedu.exam_creation.question.domain.entity.ContentEntity;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.domain.repository.IQuestionRepository;
import com.wedu.exam_creation.question.dto.mapper.QuestionDTOMapper;
import com.wedu.exam_creation.question.dto.request.ExamMatrixDetailDTO;
import com.wedu.exam_creation.questionReview.usecase.QuestionReviewService;
import com.wedu.exam_creation.user.usecase.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

class QuestionServiceTest {
    private final IQuestionRepository repository = mock(IQuestionRepository.class);
    private final QuestionDTOMapper mapper = mock(QuestionDTOMapper.class);
    private final AIQuestionGenerationService ai = mock(AIQuestionGenerationService.class);
    private final TelegramNotificationService telegram = mock(TelegramNotificationService.class);
    private final QuestionReviewService reviews = mock(QuestionReviewService.class);
    private final UserService users = mock(UserService.class);
    private final QuestionService service =
            new QuestionService(repository, mapper, ai, telegram, reviews);

    @BeforeEach
    void wireUserService() {
        ReflectionTestUtils.setField(service, "users", users);
    }

    @Test
    @DisplayName("trả về ConflictException khi câu hỏi đã được duyệt")
    void test_getPendingForEdit_001() {
        when(repository.findById("id"))
                .thenReturn(QuestionEntity.builder().status("APPROVED").build());
        assertThatThrownBy(() -> service.getPendingForEdit("id"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về ConflictException khi cập nhật nội dung không khớp phiên bản")
    void test_updatePendingContent_002() {
        Instant version = Instant.parse("2026-10-29T00:00:00Z");
        ContentDTO content = new ContentDTO();
        assertThatThrownBy(() -> service.updatePendingContent("id", content, List.of(), version))
                .isInstanceOf(ConflictException.class);
        verify(repository).updatePendingContent(eq("id"), isNull(), eq(List.of()), eq(version));
    }

    @Test
    @DisplayName("trả về DTO và metadata phân trang khi repository trả câu hỏi chờ duyệt")
    void test_findPending_003() {
        QuestionEntity entity = QuestionEntity.builder().id("question-id").build();
        PendingQuestionDTO dto = new PendingQuestionDTO();
        dto.setId("question-id");
        when(repository.countPending("search", "Đúng sai", List.of("chapter"), List.of("lesson")))
                .thenReturn(19L);
        when(repository.findPending(1, "search", "Đúng sai", List.of("chapter"), List.of("lesson")))
                .thenReturn(List.of(entity));
        when(mapper.entityToPendingDTO(entity)).thenReturn(dto);

        PendingQuestionPageDTO result =
                service.findPending(1, "search", "Đúng sai", List.of("chapter"), List.of("lesson"));

        assertThat(result.getItems()).containsExactly(dto);
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(19);
        assertThat(result.getTotalPages()).isEqualTo(2);
        verify(mapper).entityToPendingDTO(entity);
    }

    @Test
    @DisplayName("trả về tổng kết quả giữ nguyên khi trang được yêu cầu rỗng")
    void test_findPending_004() {
        when(repository.countPending("", "", List.of(), List.of())).thenReturn(9L);
        when(repository.findPending(1, "", "", List.of(), List.of())).thenReturn(List.of());

        PendingQuestionPageDTO result = service.findPending(1, "", "", List.of(), List.of());

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(9);
        assertThat(result.getTotalPages()).isEqualTo(1);
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ và lưu riêng bản sinh lại khi chỉnh sửa bản mới nhất")
    void test_updatePendingContent_005() {
        Instant version = Instant.parse("2026-10-31T00:00:00Z");
        QuestionEntity original =
                QuestionEntity.builder()
                        .id("id")
                        .status("PENDING_REVIEW")
                        .updatedAt(version)
                        .build();
        QuestionDTO candidate = new QuestionDTO();
        candidate.setId("id");
        givenPending(original);
        when(reviews.state("id")).thenReturn(new QuestionReviewStateDTO(2, 3, candidate));
        when(repository.updatePendingState("id", "PENDING_REVIEW", null, null, version))
                .thenReturn(true);
        ContentDTO content = new ContentDTO();
        content.setTemplate("Đã sửa bản mới nhất");
        service.updatePendingContent("id", content, List.of(), version, 3);
        verify(repository).updatePendingState("id", "PENDING_REVIEW", null, null, version);
        verify(repository, never()).updatePendingContent(any(), any(), any(), any());
        verify(reviews).saveCandidate("id", candidate, 3, false);
        assertThat(candidate.getQuestion().getTemplate()).isEqualTo("Đã sửa bản mới nhất");
    }

    @Test
    @DisplayName("trả về ConflictException và dừng lưu bản sinh lại khi guard cập nhật thất bại")
    void test_updatePendingContent_006() {
        Instant version = Instant.parse("2026-10-31T00:00:00Z");
        givenPending(
                QuestionEntity.builder()
                        .id("id")
                        .status("PENDING_REVIEW")
                        .updatedAt(version)
                        .build());
        when(reviews.state("id")).thenReturn(new QuestionReviewStateDTO(1, 1, new QuestionDTO()));
        assertThatThrownBy(
                        () ->
                                service.updatePendingContent(
                                        "id", new ContentDTO(), List.of(), version, 1))
                .isInstanceOf(ConflictException.class);
        verify(reviews, never()).saveCandidate(any(), any(), anyLong(), anyBoolean());
    }

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

    @ParameterizedTest
    @CsvSource({"FREE,true", "PLUS,false"})
    @DisplayName("trả về ConflictException khi tài khoản đã mất PLUS hoặc bị khóa")
    void test_generateExamQuestions_007(String currentAccountType, boolean active) {
        givenBankWithoutMatch();
        CommonUserResponseAllDTO current = new CommonUserResponseAllDTO();
        current.setAccountType(currentAccountType);
        current.setIsActive(active);
        when(users.findById("teacher")).thenReturn(current);

        assertThatThrownBy(() -> service.generateExamQuestions("PLUS", chapters, matrix, "teacher"))
                .isInstanceOf(ConflictException.class);
        verifyNoInteractions(ai);
    }

    @Test
    @DisplayName("trả về lỗi AI gốc khi tài khoản PLUS đang hoạt động và nhà cung cấp thất bại")
    void test_generateExamQuestions_008() throws Exception {
        givenBankWithoutMatch();
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
    @DisplayName("trả về thông báo thiếu câu khi tài khoản FREE cần thêm câu hỏi")
    void test_generateExamQuestions_009() throws Exception {
        givenBankWithoutMatch();

        ExamGeneratedDTO result =
                service.generateExamQuestions("FREE", chapters, matrix, "teacher");
        assertThat(result.getErrors()).singleElement().asString().contains("Thiếu 1 câu hỏi");
        verifyNoInteractions(ai, users);
    }

    @Test
    @DisplayName("trả về ID đã lưu khi nhập DOCX với trạng thái APPROVED")
    void test_insertFromDocx_010() {
        NewQuestionDTO input = new NewQuestionDTO();
        QuestionEntity mapped = QuestionEntity.builder().build();
        when(mapper.newQuestionDTOToEntity(input)).thenReturn(mapped);
        when(repository.saveQuestions(List.of(mapped))).thenReturn(List.of("docx-1"));

        assertThat(service.insertFromDocx(List.of(input))).containsExactly("docx-1");

        assertThat(mapped.getStatus()).isEqualTo("APPROVED");
        verifyNoInteractions(ai, telegram, reviews);
    }

    @Test
    @DisplayName("trả về DTO kèm bản sinh lại khi mở câu hỏi chờ duyệt đã có review")
    void test_getPendingForEdit_011() {
        QuestionEntity pending = QuestionEntity.builder().id("id").status("PENDING_REVIEW").build();
        givenPending(pending);
        QuestionDTO candidate = new QuestionDTO();
        when(reviews.state("id")).thenReturn(new QuestionReviewStateDTO(2, 3, candidate));

        PendingQuestionDTO result = service.getPendingForEdit("id");

        assertThat(result.getRegenerationCount()).isEqualTo(2);
        assertThat(result.getReviewRevision()).isEqualTo(3);
        assertThat(result.getLatestRegeneratedQuestion()).isSameAs(candidate);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi duyệt bản sinh lại và lưu nội dung đã ánh xạ")
    void test_commitPendingReview_012() {
        Instant version = Instant.parse("2026-11-22T00:00:00Z");
        QuestionDTO candidate = new QuestionDTO();
        ContentDTO question = new ContentDTO();
        ContentDTO option = new ContentDTO();
        question.setTemplate("Câu mới");
        option.setTemplate("Phương án");
        candidate.setQuestion(question);
        candidate.setOptions(List.of(option));
        ContentEntity mappedQuestion = new ContentEntity();
        ContentEntity mappedOption = new ContentEntity();
        when(mapper.contentDTOToEntity(question)).thenReturn(mappedQuestion);
        when(mapper.contentDTOToEntity(option)).thenReturn(mappedOption);
        when(repository.updatePendingState(
                        "id", "APPROVED", mappedQuestion, List.of(mappedOption), version))
                .thenReturn(true);

        service.commitPendingReview("id", "APPROVED", candidate, version);

        verify(repository)
                .updatePendingState(
                        "id", "APPROVED", mappedQuestion, List.of(mappedOption), version);
    }

    @Test
    @DisplayName("trả về các DTO đã ánh xạ khi tìm câu hỏi theo danh sách ID")
    void test_findByIds_013() {
        QuestionEntity entity = QuestionEntity.builder().id("id").build();
        QuestionDTO dto = new QuestionDTO();
        when(repository.findByIds(List.of("id"))).thenReturn(List.of(entity));
        when(mapper.entityToCommonDTO(entity)).thenReturn(dto);

        assertThat(service.findByIds(List.of("id"))).containsExactly(dto);
    }

    @Test
    @DisplayName("trả về đề có ID câu mới khi PLUS được bù câu bằng AI và tạo review")
    void test_generateExamQuestions_014() throws Exception {
        givenBankWithoutMatch();
        CommonUserResponseAllDTO current = new CommonUserResponseAllDTO();
        current.setAccountType("PLUS");
        current.setIsActive(true);
        when(users.findById("teacher")).thenReturn(current);
        AIQuestionResponseDTO generated =
                new AIQuestionResponseDTO(
                        "chapter",
                        "lesson",
                        "exercise",
                        "difficulty",
                        "outcome",
                        "type",
                        new ContentDTO(),
                        List.of());
        when(ai.generateQuestions(anyList())).thenReturn(List.of(generated));
        QuestionEntity mapped = QuestionEntity.builder().build();
        when(mapper.newQuestionDTOToEntity(any(NewQuestionDTO.class))).thenReturn(mapped);
        when(repository.saveQuestions(List.of(mapped))).thenReturn(List.of("ai-1"));

        ExamGeneratedDTO result =
                service.generateExamQuestions("PLUS", chapters, matrix, "teacher");

        assertThat(result.getQuestions())
                .singleElement()
                .satisfies(group -> assertThat(group.getQuestionIds()).containsExactly("ai-1"));
        assertThat(mapped.getStatus()).isEqualTo("PENDING_REVIEW");
        ArgumentCaptor<List<GenerateQuestionRequestDTO>> requests = ArgumentCaptor.captor();
        verify(ai).generateQuestions(requests.capture());
        assertThat(requests.getValue())
                .singleElement()
                .satisfies(
                        request -> {
                            assertThat(request.getNumberOfQuestions()).isEqualTo(1);
                            assertThat(request.getLessonId()).isEqualTo("lesson");
                        });
        InOrder order = inOrder(repository, reviews, telegram);
        order.verify(repository).saveQuestions(List.of(mapped));
        order.verify(reviews).createForQuestions(List.of("ai-1"));
        order.verify(telegram).notifyPendingReview(List.of("ai-1"));
    }

    @Test
    @DisplayName("trả về null khi ma trận không có ô yêu cầu câu hỏi")
    void test_generateExamQuestions_015() throws Exception {
        assertThat(service.generateExamQuestions("PLUS", chapters, List.of(), "teacher")).isNull();

        verifyNoInteractions(repository, ai, users, reviews, telegram);
    }

    @Test
    @DisplayName("trả về NotFoundException gốc khi ngân hàng không có câu nào cho các bài yêu cầu")
    void test_generateExamQuestions_016() {
        NotFoundException missing = new NotFoundException("Không tìm thấy câu hỏi nào");
        when(repository.findByLessonIds(List.of("lesson"))).thenThrow(missing);

        assertThatThrownBy(() -> service.generateExamQuestions("PLUS", chapters, matrix, "teacher"))
                .isSameAs(missing);
        verifyNoInteractions(ai, users, reviews, telegram);
    }

    @Test
    @DisplayName("trả về mỗi ID một lần khi hai ô ma trận cùng khớp một câu trong ngân hàng")
    void test_generateExamQuestions_017() throws Exception {
        QuestionEntity matching =
                QuestionEntity.builder()
                        .id("bank-1")
                        .lessonId("lesson")
                        .exerciseType("exercise")
                        .difficultyLevel("difficulty")
                        .questionType("type")
                        .learningOutcomes(List.of("outcome"))
                        .build();
        when(repository.findByLessonIds(List.of("lesson"))).thenReturn(List.of(matching));

        ExamGeneratedDTO result =
                service.generateExamQuestions(
                        "FREE", chapters, List.of(matrix.get(0), matrix.get(0)));

        assertThat(result.getQuestions())
                .singleElement()
                .satisfies(group -> assertThat(group.getQuestionIds()).containsExactly("bank-1"));
        assertThat(result.getErrors()).hasSize(1);
        verifyNoInteractions(ai, users);
    }

    @Test
    @DisplayName(
            "trả về không có ngoại lệ và lưu nội dung gốc khi câu chờ duyệt chưa có bản sinh lại")
    void test_updatePendingContent_018() {
        Instant version = Instant.parse("2026-11-22T00:00:00Z");
        givenPending(
                QuestionEntity.builder()
                        .id("id")
                        .status("PENDING_REVIEW")
                        .updatedAt(version)
                        .build());
        ContentDTO content = new ContentDTO();
        ContentEntity mapped = new ContentEntity();
        when(mapper.contentDTOToEntity(content)).thenReturn(mapped);
        when(repository.updatePendingContent("id", mapped, List.of(), version)).thenReturn(true);

        service.updatePendingContent("id", content, List.of(), version, 0);

        verify(repository).updatePendingContent("id", mapped, List.of(), version);
        verify(reviews, never()).saveCandidate(any(), any(), anyLong(), anyBoolean());
    }

    private void givenPending(QuestionEntity entity) {
        PendingQuestionDTO dto = new PendingQuestionDTO();
        dto.setId(entity.getId());
        dto.setUpdatedAt(entity.getUpdatedAt());
        when(repository.findById(entity.getId())).thenReturn(entity);
        when(mapper.entityToPendingDTO(entity)).thenReturn(dto);
    }

    private void givenBankWithoutMatch() {
        QuestionEntity irrelevant = QuestionEntity.builder().lessonId("another-lesson").build();
        when(repository.findByLessonIds(List.of("lesson"))).thenReturn(List.of(irrelevant));
    }
}
