package com.wedu.exam_creation.moderator.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.chapter.usecase.ChapterService;
import com.wedu.exam_creation.common.dto.user.response.CommonUserResponseAllDTO;
import com.wedu.exam_creation.common.exception.BadRequestException;
import com.wedu.exam_creation.common.exception.ForbiddenException;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.importer.service.ImporterService;
import com.wedu.exam_creation.mail.service.MailService;
import com.wedu.exam_creation.question.usecase.QuestionService;
import com.wedu.exam_creation.storage.dto.response.S3ObjectInfo;
import com.wedu.exam_creation.storage.service.S3Service;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkClientException;

class ModeratorUsecaseTest {
    private final ImporterService importer = mock(ImporterService.class);
    private final S3Service storage = mock(S3Service.class);
    private final ChapterService chapters = mock(ChapterService.class);
    private final QuestionService questions = mock(QuestionService.class);
    private final ModeratorUsecase usecase =
            new ModeratorUsecase(importer, storage, mock(MailService.class), chapters, questions);

    @Test
    @DisplayName("trả về danh sách tệp do S3 cung cấp khi lấy thư mục question-files")
    void test_listQuestionFiles_001() {
        List<S3ObjectInfo> files =
                List.of(
                        new S3ObjectInfo(
                                "question-files/input.docx", "input.docx", 12, null, null));
        when(storage.listQuestionFiles("question-files")).thenReturn(files);

        assertThat(usecase.listQuestionFiles()).containsExactlyElementsOf(files);
    }

    @Test
    @DisplayName("trả về InternalServerException với thông báo chung khi S3 không liệt kê được tệp")
    void test_listQuestionFiles_002() {
        when(storage.listQuestionFiles("question-files"))
                .thenThrow(SdkClientException.create("storage unavailable"));

        assertThatThrownBy(usecase::listQuestionFiles)
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Không thể lấy danh sách tệp tin câu hỏi");
    }

    @ParameterizedTest
    @CsvSource({"201,1", "1,101"})
    @DisplayName("trả về BadRequestException khi từ khóa hoặc loại câu vượt giới hạn độ dài")
    void test_listPendingQuestions_003(int searchLength, int typeLength) {
        assertThatThrownBy(
                        () ->
                                usecase.listPendingQuestions(
                                        "0", "a".repeat(searchLength), "b".repeat(typeLength)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Bộ lọc vượt quá độ dài cho phép");

        verifyNoInteractions(chapters, questions, storage);
    }

    @Test
    @DisplayName("trả về ID do importer tạo khi moderator nhập tài liệu hợp lệ")
    void test_parseDocument_004() throws Exception {
        CommonUserResponseAllDTO moderator = new CommonUserResponseAllDTO();
        moderator.setRole("ROLE_MODERATOR");
        MultipartFile file = new MockMultipartFile("file", new byte[] {1});
        when(importer.parseDocument(file, "physics")).thenReturn(List.of("q1"));

        assertThat(usecase.parseDocument(moderator, file, "physics")).containsExactly("q1");
    }

    @Test
    @DisplayName("trả về ForbiddenException khi người nhập tài liệu có vai trò giáo viên")
    void test_parseDocument_005() {
        CommonUserResponseAllDTO teacher = new CommonUserResponseAllDTO();
        teacher.setRole("ROLE_TEACHER");
        MultipartFile file = new MockMultipartFile("file", new byte[] {1});

        assertThatThrownBy(() -> usecase.parseDocument(teacher, file, "physics"))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(importer);
    }
}
