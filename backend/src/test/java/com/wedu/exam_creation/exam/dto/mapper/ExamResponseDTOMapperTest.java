package com.wedu.exam_creation.exam.dto.mapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.common.dto.exam.response.*;
import com.wedu.exam_creation.common.dto.question.response.*;
import com.wedu.exam_creation.exam.domain.entity.*;
import com.wedu.exam_creation.exam.dto.response.*;
import com.wedu.exam_creation.storage.service.S3Service;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExamResponseDTOMapperTest {
    private final ExamResponseDTOMapper mapper = new ExamResponseDTOMapper();
    private final S3Service storage = mock(S3Service.class);

    private ExamEntity exam() {
        return new ExamEntity(
                "exam",
                "owner",
                "Đề",
                List.of(
                        new ChapterExamEntity(
                                List.of(new LessonExamEntity("l1", "Bài 1", List.of(), List.of())),
                                "c1",
                                "Chương 1"),
                        new ChapterExamEntity(
                                List.of(new LessonExamEntity("l2", "Bài 2", List.of(), List.of())),
                                "c2",
                                "Chương 2")),
                List.of(
                        new QuestionExamEntity("Đúng sai", "Nhận biết", List.of("q1", "q2")),
                        new QuestionExamEntity("Trả lời ngắn", "Thông hiểu", List.of("q3"))),
                null,
                null);
    }

    private QuestionDTO question(
            String id, String chapter, String lesson, String type, String difficulty) {
        return new QuestionDTO(
                id,
                chapter,
                lesson,
                "Dạng",
                difficulty,
                List.of("Yêu cầu"),
                type,
                new ContentDTO("Câu " + id, new VariablesDTO(Map.of(), Map.of())),
                List.of(new ContentDTO("Phương án " + id, new VariablesDTO(Map.of(), Map.of()))));
    }

    private QuestionDetailResponseDTO detail(QuestionDTO question) {
        return new QuestionDetailResponseDTO(
                question.getId(), question.getQuestion(), question.getOptions());
    }

    @Test
    @DisplayName(
            "trả về nhóm theo loại và độ khó chỉ chứa câu thuộc chương bài của đề khi có nhiều nhóm và câu ngoài phạm vi")
    void test_convertToExamDetailResponse_001() {
        QuestionDTO first = question("q1", "c1", "l1", "Đúng sai", "Nhận biết");
        QuestionDTO second = question("q2", "c2", "l2", "Đúng sai", "Nhận biết");
        QuestionDTO difficult = question("q3", "c1", "l1", "Đúng sai", "Thông hiểu");
        QuestionDTO anotherType = question("q4", "c2", "l2", "Trả lời ngắn", "Nhận biết");
        QuestionDTO wrongChapter = question("outside1", "other", "l1", "Đúng sai", "Nhận biết");
        QuestionDTO wrongLesson = question("outside2", "c1", "l2", "Đúng sai", "Nhận biết");
        ExamDetailDTO result =
                mapper.convertToExamDetailResponse(
                        exam(),
                        List.of(first, second, difficult, anotherType, wrongChapter, wrongLesson),
                        storage);
        assertThat(result.getId()).isEqualTo("exam");
        assertThat(result.getUserId()).isEqualTo("owner");
        assertThat(result.getName()).isEqualTo("Đề");
        assertThat(result.getGroups())
                .containsExactlyInAnyOrder(
                        new ExamQuestionDTO(
                                "Đúng sai", "Nhận biết", List.of(detail(first), detail(second))),
                        new ExamQuestionDTO("Đúng sai", "Thông hiểu", List.of(detail(difficult))),
                        new ExamQuestionDTO(
                                "Trả lời ngắn", "Nhận biết", List.of(detail(anotherType))));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về groups rỗng khi không có câu hỏi")
    void test_convertToExamDetailResponse_002() {
        assertThat(mapper.convertToExamDetailResponse(exam(), List.of(), storage))
                .isEqualTo(new ExamDetailDTO("exam", "owner", "Đề", List.of()));
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName("trả về groups rỗng khi mọi câu hỏi đều ngoài chương bài của đề")
    void test_convertToExamDetailResponse_003() {
        assertThat(
                        mapper.convertToExamDetailResponse(
                                        exam(),
                                        List.of(
                                                question(
                                                        "outside",
                                                        "other",
                                                        "l1",
                                                        "Đúng sai",
                                                        "Nhận biết")),
                                        storage)
                                .getGroups())
                .isEmpty();
        verifyNoInteractions(storage);
    }

    @Test
    @DisplayName(
            "trả về ảnh đã ký và giữ math template cùng dữ liệu gốc khi câu và phương án có placeholders")
    void test_convertToExamDetailResponse_004() {
        QuestionDTO question = question("q1", "c1", "l1", "Đúng sai", "Nhận biết");
        ContentDTO body =
                new ContentDTO(
                        "<math_1> <img_1>",
                        new VariablesDTO(
                                Map.of("math_1", "x^2"), Map.of("img_1", "media/body.png")));
        ContentDTO option =
                new ContentDTO(
                        "<img_2> <math_2>",
                        new VariablesDTO(
                                Map.of("math_2", "y^2"), Map.of("img_2", "media/option.png")));
        question.setQuestion(body);
        question.setOptions(List.of(option));
        when(storage.generatePresignedUrl("media/body.png")).thenReturn("signed-body");
        when(storage.generatePresignedUrl("media/option.png")).thenReturn("signed-option");
        QuestionDetailResponseDTO result =
                mapper.convertToExamDetailResponse(exam(), List.of(question), storage)
                        .getGroups()
                        .get(0)
                        .getQuestions()
                        .get(0);
        assertThat(result)
                .isEqualTo(
                        new QuestionDetailResponseDTO(
                                "q1",
                                new ContentDTO(
                                        "<math_1> <img_1>",
                                        new VariablesDTO(
                                                Map.of("math_1", "x^2"),
                                                Map.of("img_1", "signed-body"))),
                                List.of(
                                        new ContentDTO(
                                                "<img_2> <math_2>",
                                                new VariablesDTO(
                                                        Map.of("math_2", "y^2"),
                                                        Map.of("img_2", "signed-option"))))));
        assertThat(question.getQuestion()).isSameAs(body);
        assertThat(body.getVariables().getImage())
                .containsExactlyEntriesOf(Map.of("img_1", "media/body.png"));
        assertThat(option.getVariables().getImage())
                .containsExactlyEntriesOf(Map.of("img_2", "media/option.png"));
        assertThat(result.getQuestion()).isNotSameAs(body);
        assertThat(result.getOptions().get(0)).isNotSameAs(option);
        verify(storage).generatePresignedUrl("media/body.png");
        verify(storage).generatePresignedUrl("media/option.png");
        verifyNoMoreInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("trả về lỗi S3 và giữ object key gốc khi ký ảnh câu hỏi hoặc phương án thất bại")
    void test_convertToExamDetailResponse_005(boolean bodyFails) {
        QuestionDTO question = question("q1", "c1", "l1", "Đúng sai", "Nhận biết");
        ContentDTO content =
                new ContentDTO(
                        "<img_1>", new VariablesDTO(Map.of(), Map.of("img_1", "media/fail.png")));
        if (bodyFails) question.setQuestion(content);
        else question.setOptions(List.of(content));
        IllegalStateException failure = new IllegalStateException("storage failed");
        when(storage.generatePresignedUrl("media/fail.png")).thenThrow(failure);
        assertThatThrownBy(
                        () ->
                                mapper.convertToExamDetailResponse(
                                        exam(), List.of(question), storage))
                .isSameAs(failure);
        assertThat(content.getVariables().getImage())
                .containsExactlyEntriesOf(Map.of("img_1", "media/fail.png"));
    }

    @Test
    @DisplayName("trả về metadata chương bài và tổng số ID câu hỏi khi đề có nhiều nhóm")
    void test_convertToExamResponse_001() {
        assertThat(mapper.convertToExamResponse(exam()))
                .isEqualTo(
                        new ExamDTO(
                                "exam",
                                "Đề",
                                List.of(
                                        new ExamChapterDTO("c1", List.of("l1")),
                                        new ExamChapterDTO("c2", List.of("l2"))),
                                3));
    }

    @Test
    @DisplayName("trả về danh sách chương rỗng và số câu 0 khi đề không có chương hoặc nhóm câu")
    void test_convertToExamResponse_002() {
        assertThat(
                        mapper.convertToExamResponse(
                                new ExamEntity(
                                        "exam", "owner", "Đề", List.of(), List.of(), null, null)))
                .isEqualTo(new ExamDTO("exam", "Đề", List.of(), 0));
    }

    @Test
    @DisplayName("trả về số câu 0 và lessonIds rỗng khi chương chưa có bài và nhóm câu chưa có ID")
    void test_convertToExamResponse_003() {
        ExamEntity entity =
                new ExamEntity(
                        "exam",
                        "owner",
                        "Đề",
                        List.of(new ChapterExamEntity(List.of(), "c", "Chương")),
                        List.of(new QuestionExamEntity("Đúng sai", "Nhận biết", List.of())),
                        null,
                        null);
        assertThat(mapper.convertToExamResponse(entity))
                .isEqualTo(
                        new ExamDTO("exam", "Đề", List.of(new ExamChapterDTO("c", List.of())), 0));
    }
}
