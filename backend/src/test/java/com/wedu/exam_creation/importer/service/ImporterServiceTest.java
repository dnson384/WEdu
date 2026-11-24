package com.wedu.exam_creation.importer.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.chapter.NewChapterDTO;
import com.wedu.exam_creation.common.dto.chapter.response.SavedChapterResponse;
import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.importer.dto.parsed.*;
import com.wedu.exam_creation.importer.infrastructure.pandoc.PandocConverter;
import com.wedu.exam_creation.question.usecase.QuestionService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

class ImporterServiceTest {
    private final ChapterService chapters = mock(ChapterService.class);
    private final QuestionService questions = mock(QuestionService.class);
    private final PandocConverter parser = mock(PandocConverter.class);
    private final ImporterService importer = new ImporterService(chapters, questions, parser);
    private final byte[] docx = {1, 2, 3};
    private final MultipartFile file = new MockMultipartFile("file", docx);

    @Test
    @DisplayName("trả về ID câu hỏi đã lưu khi parser trả chương, bài và câu hỏi có biến")
    void test_parseDocument_001() throws Exception {
        when(parser.parse(docx)).thenReturn(parsedDocument());
        when(chapters.insert(any(NewChapterDTO.class)))
                .thenReturn(new SavedChapterResponse("chapter-1", "lesson-1"));
        when(questions.insertFromDocx(anyList())).thenReturn(List.of("question-1"));

        assertThat(importer.parseDocument(file, "physics")).containsExactly("question-1");

        ArgumentCaptor<NewChapterDTO> chapterCaptor = ArgumentCaptor.forClass(NewChapterDTO.class);
        ArgumentCaptor<List<NewQuestionDTO>> questionCaptor = ArgumentCaptor.captor();
        InOrder order = inOrder(parser, chapters, questions);
        order.verify(parser).parse(docx);
        order.verify(chapters).insert(chapterCaptor.capture());
        order.verify(questions).insertFromDocx(questionCaptor.capture());
        NewChapterDTO chapter = chapterCaptor.getValue();
        assertThat(chapter.getSubject()).isEqualTo("physics");
        assertThat(chapter.getName()).isEqualTo("Electricity");
        assertThat(chapter.getLessons())
                .singleElement()
                .satisfies(
                        lesson -> {
                            assertThat(lesson.getName()).isEqualTo("Circuits");
                            assertThat(lesson.getBankStats())
                                    .singleElement()
                                    .satisfies(
                                            stat -> {
                                                assertThat(stat.getExerciseType())
                                                        .isEqualTo("Calculation");
                                                assertThat(stat.getDifficultyLevels())
                                                        .containsExactly("Basic");
                                                assertThat(stat.getLearningOutcomes())
                                                        .containsExactly("Apply Ohm's law");
                                                assertThat(stat.getQuestionType())
                                                        .isEqualTo("Multiple choice");
                                                assertThat(stat.getCount()).isEqualTo(1);
                                            });
                        });
        assertThat(questionCaptor.getValue())
                .singleElement()
                .satisfies(
                        question -> {
                            assertThat(question.getChapterId()).isEqualTo("chapter-1");
                            assertThat(question.getLessonId()).isEqualTo("lesson-1");
                            assertThat(question.getExerciseType()).isEqualTo("Calculation");
                            assertThat(question.getDifficultyLevel()).isEqualTo("Basic");
                            assertThat(question.getLearningOutcomes())
                                    .containsExactly("Apply Ohm's law");
                            assertThat(question.getQuestionType()).isEqualTo("Multiple choice");
                            assertThat(question.getQuestion().getTemplate())
                                    .isEqualTo("Find <math_0> in <img_0>");
                            assertThat(question.getQuestion().getVariables().getMath())
                                    .containsEntry("math_0", "I");
                            assertThat(question.getQuestion().getVariables().getImage())
                                    .containsEntry("img_0", "media/circuit.png");
                            assertThat(question.getOptions())
                                    .singleElement()
                                    .satisfies(
                                            option -> {
                                                assertThat(option.getTemplate())
                                                        .isEqualTo("<math_0>");
                                                assertThat(option.getVariables().getMath())
                                                        .containsEntry("math_0", "2A");
                                            });
                        });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("trả về IllegalArgumentException khi môn học bị thiếu hoặc để trống")
    void test_parseDocument_002(String subject) {
        MultipartFile unreadFile = mock(MultipartFile.class);

        assertThatThrownBy(() -> importer.parseDocument(unreadFile, subject))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Không tồn tại môn học");

        verifyNoInteractions(unreadFile, parser, chapters, questions);
    }

    @Test
    @DisplayName("trả về IllegalArgumentException khi file rỗng")
    void test_parseDocument_003() {
        MultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> importer.parseDocument(empty, "physics"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("File không tồn tại");

        verifyNoInteractions(parser, chapters, questions);
    }

    @Test
    @DisplayName("trả về lỗi đọc file khi MultipartFile không cung cấp được nội dung")
    void test_parseDocument_004() throws IOException {
        MultipartFile unreadable = mock(MultipartFile.class);
        IOException failure = new IOException("read failure");
        when(unreadable.getBytes()).thenThrow(failure);

        assertThatThrownBy(() -> importer.parseDocument(unreadable, "physics")).isSameAs(failure);

        verifyNoInteractions(parser, chapters, questions);
    }

    @Test
    @DisplayName("trả về lỗi parser và không lưu dữ liệu khi không chuyển đổi được tài liệu")
    void test_parseDocument_005() throws Exception {
        IOException failure = new IOException("parser failure");
        when(parser.parse(docx)).thenThrow(failure);

        assertThatThrownBy(() -> importer.parseDocument(file, "physics")).isSameAs(failure);

        verifyNoInteractions(chapters, questions);
    }

    @Test
    @DisplayName("trả về lỗi lưu chương và không lưu câu hỏi khi không có ID chương bài")
    void test_parseDocument_006() throws Exception {
        when(parser.parse(docx)).thenReturn(parsedDocument());
        IllegalStateException failure = new IllegalStateException("chapter storage failure");
        when(chapters.insert(any(NewChapterDTO.class))).thenThrow(failure);

        assertThatThrownBy(() -> importer.parseDocument(file, "physics")).isSameAs(failure);

        verifyNoInteractions(questions);
    }

    @Test
    @DisplayName("trả về lỗi lưu câu hỏi khi dịch vụ câu hỏi từ chối dữ liệu DOCX")
    void test_parseDocument_007() throws Exception {
        when(parser.parse(docx)).thenReturn(parsedDocument());
        when(chapters.insert(any(NewChapterDTO.class)))
                .thenReturn(new SavedChapterResponse("chapter-1", "lesson-1"));
        IllegalStateException failure = new IllegalStateException("question storage failure");
        when(questions.insertFromDocx(anyList())).thenThrow(failure);

        assertThatThrownBy(() -> importer.parseDocument(file, "physics")).isSameAs(failure);
    }

    private ParsedDataOutput parsedDocument() {
        BankStatDTO stat =
                new BankStatDTO(
                        "Calculation",
                        List.of("Basic"),
                        List.of("Apply Ohm's law"),
                        "Multiple choice",
                        1);
        NewChapterImporterDTO chapter =
                new NewChapterImporterDTO(
                        "Electricity",
                        List.of(new LessonDataImporterDTO("Circuits", List.of(stat))));
        NewQuestionImporterDTO question =
                NewQuestionImporterDTO.builder()
                        .exerciseType("Calculation")
                        .difficultyLevel("Basic")
                        .learningOutcomes(List.of("Apply Ohm's law"))
                        .questionType("Multiple choice")
                        .question(
                                new QuestionContentImporterDTO(
                                        "Find <math_0> in <img_0>",
                                        new VariablesDTO(
                                                Map.of("math_0", "I"),
                                                Map.of("img_0", "media/circuit.png"))))
                        .options(
                                List.of(
                                        new OptionsDataImporterDTO(
                                                "<math_0>",
                                                new VariablesDTO(
                                                        Map.of("math_0", "2A"), Map.of()))))
                        .build();
        return new ParsedDataOutput(List.of(question), chapter);
    }
}
