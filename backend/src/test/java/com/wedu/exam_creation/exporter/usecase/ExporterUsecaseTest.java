package com.wedu.exam_creation.exporter.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.wedu.exam_creation.common.dto.exam.response.ExamDetailDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamMatrixDTO;
import com.wedu.exam_creation.common.dto.exam.response.ExamQuestionDTO;
import com.wedu.exam_creation.common.dto.exam.response.QuestionDetailResponseDTO;
import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import com.wedu.exam_creation.common.dto.question.response.VariablesDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.common.exception.NotFoundException;
import com.wedu.exam_creation.common.exception.UnprocessableEntityException;
import com.wedu.exam_creation.exam.usecase.ExamService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

class ExporterUsecaseTest {
    private static final String BASE_URL = "https://exporter.example.test/exporter/";
    private static final MediaType DOCX =
            MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final byte[] DOCUMENT_BYTES = {80, 75, 3, 4};
    private final ExamService exams = mock(ExamService.class);
    private final ExporterUsecase usecase = new ExporterUsecase(BASE_URL, exams);
    private final MockRestServiceServer server =
            MockRestServiceServer.bindTo(
                            (RestTemplate) ReflectionTestUtils.getField(usecase, "restTemplate"))
                    .build();

    @Test
    @DisplayName("trả về nguyên dữ liệu DOCX khi gửi snapshot ma trận của đề thuộc chủ phiên")
    void test_exportMatrix_001() {
        prepareMatrix();
        server.expect(requestTo(BASE_URL + "matrix"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.examName").value("Đề tiếng Việt"))
                .andExpect(jsonPath("$.matrix[0].selectedCount").value(0))
                .andExpect(jsonPath("$.matrixDetails[0].learningOutcome").value("Yêu cầu"))
                .andRespond(withSuccess(DOCUMENT_BYTES, DOCX));

        assertThat(usecase.exportMatrix("owner", "exam")).isEqualTo(DOCUMENT_BYTES);

        server.verify();
        verify(exams).getExamMatrixById("owner", "exam");
    }

    @ParameterizedTest
    @ValueSource(strings = {"json", "missing-type", "empty"})
    @DisplayName(
            "trả về InternalServerException khi phản hồi xuất ma trận thiếu nội dung hoặc sai loại DOCX")
    void test_exportMatrix_002(String response) {
        prepareMatrix();
        DefaultResponseCreator invalid =
                switch (response) {
                    case "json" -> withSuccess("{}", MediaType.APPLICATION_JSON);
                    case "missing-type" -> withSuccess(DOCUMENT_BYTES, null);
                    case "empty" -> withSuccess(new byte[0], DOCX);
                    default -> throw new IllegalArgumentException(response);
                };
        server.expect(requestTo(BASE_URL + "matrix")).andRespond(invalid);

        assertThatThrownBy(() -> usecase.exportMatrix("owner", "exam"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Dịch vụ xuất ma trận không trả về file DOCX hợp lệ");
        server.verify();
    }

    @Test
    @DisplayName("trả về thông báo thử lại khi dịch vụ xuất ma trận phản hồi HTTP 500")
    void test_exportMatrix_003() {
        prepareMatrix();
        server.expect(requestTo(BASE_URL + "matrix"))
                .andRespond(withServerError().body("upstream fixture"));

        assertThatThrownBy(() -> usecase.exportMatrix("owner", "exam"))
                .isInstanceOf(InternalServerException.class)
                .hasMessage("Không thể xuất ma trận đề. Vui lòng thử lại sau");
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi thiếu snapshot gốc khi dữ liệu đề không đủ để xuất ma trận")
    void test_exportMatrix_004() {
        UnprocessableEntityException failure = new UnprocessableEntityException("Thiếu ma trận");
        when(exams.getExamMatrixById("owner", "exam")).thenThrow(failure);

        assertThatThrownBy(() -> usecase.exportMatrix("owner", "exam")).isSameAs(failure);
        server.verify();
    }

    @Test
    @DisplayName(
            "trả về nguyên DOCX khi gộp nhóm câu theo loại và gửi ảnh câu hỏi cùng phương án dưới dạng Base64")
    void test_exportExam_005() {
        String questionImage = "https://media.example.test/question.png";
        String optionImage = "https://media.example.test/option.png";
        ContentDTO question =
                new ContentDTO(
                        "Hình <img_1> và <math_1>",
                        new VariablesDTO(Map.of("math_1", "x^2"), Map.of("img_1", questionImage)));
        ContentDTO option =
                new ContentDTO(
                        "Hình <img_1>", new VariablesDTO(Map.of(), Map.of("img_1", optionImage)));
        List<ExamQuestionDTO> groups =
                List.of(
                        group(
                                "Đúng sai",
                                "Nhận biết",
                                new QuestionDetailResponseDTO("q1", question, List.of(option))),
                        group("Nhiều lựa chọn", "Thông hiểu", plainQuestion("q2")),
                        group(
                                "Đúng sai",
                                "Vận dụng",
                                new QuestionDetailResponseDTO("q3", question, List.of())),
                        group("Trả lời ngắn", "Nhận biết", plainQuestion("q4")));
        when(exams.getExamById("owner", "exam"))
                .thenReturn(new ExamDetailDTO("exam", "owner", "Tên đề đã lưu", groups));
        server.expect(requestTo(questionImage))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {1, 2, 3}, MediaType.IMAGE_PNG));
        server.expect(requestTo(optionImage))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(new byte[] {4, 5}, MediaType.IMAGE_PNG));
        server.expect(requestTo(BASE_URL + "exam"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.examName").value("Tên đề đã lưu"))
                .andExpect(jsonPath("$.questionsSorted.length()").value(3))
                .andExpect(jsonPath("$.questionsSorted[0].questionType").value("Nhiều lựa chọn"))
                .andExpect(jsonPath("$.questionsSorted[1].questionType").value("Đúng sai"))
                .andExpect(jsonPath("$.questionsSorted[2].questionType").value("Trả lời ngắn"))
                .andExpect(jsonPath("$.questionsSorted[1].questionsData.length()").value(2))
                .andExpect(jsonPath("$.questionsSorted[1].questionsData[0].id").value("q1"))
                .andExpect(jsonPath("$.questionsSorted[1].questionsData[1].id").value("q3"))
                .andExpect(
                        jsonPath("$.questionsSorted[1].questionsData[0].question.template")
                                .value(question.getTemplate()))
                .andExpect(
                        jsonPath("$.questionsSorted[1].questionsData[0].options[0].template")
                                .value(option.getTemplate()))
                .andExpect(
                        jsonPath("$.imageCache.questions['" + questionImage + "']").value("AQID"))
                .andExpect(jsonPath("$.imageCache.options['" + optionImage + "']").value("BAU="))
                .andRespond(withSuccess(DOCUMENT_BYTES, DOCX));

        assertThat(usecase.exportExam("owner", "exam")).isEqualTo(DOCUMENT_BYTES);

        server.verify();
        verify(exams).getExamById("owner", "exam");
    }

    @Test
    @DisplayName("trả về lỗi không tìm thấy gốc khi mã đề không khớp đề thuộc chủ phiên")
    void test_exportExam_006() {
        NotFoundException failure = new NotFoundException("Không tồn tại bài kiểm tra");
        when(exams.getExamById("owner", "exam")).thenThrow(failure);

        assertThatThrownBy(() -> usecase.exportExam("owner", "exam")).isSameAs(failure);
        server.verify();
    }

    @Test
    @DisplayName("trả về lỗi HTTP từ dịch vụ xuất khi không tạo được DOCX đề thi")
    void test_exportExam_007() {
        when(exams.getExamById("owner", "exam"))
                .thenReturn(new ExamDetailDTO("exam", "owner", "Đề", List.of()));
        server.expect(requestTo(BASE_URL + "exam"))
                .andExpect(jsonPath("$.imageCache.questions").isEmpty())
                .andExpect(jsonPath("$.imageCache.options").isEmpty())
                .andRespond(withServerError());

        assertThatThrownBy(() -> usecase.exportExam("owner", "exam"))
                .isInstanceOf(HttpServerErrorException.class);
        server.verify();
    }

    private void prepareMatrix() {
        when(exams.getExamMatrixById("owner", "exam"))
                .thenReturn(
                        new ExamMatrixDTO(
                                "Đề tiếng Việt",
                                List.of(
                                        new ExamMatrixDTO.MatrixRowDTO(
                                                "Chương", "Bài", "Đúng sai", "Nhận biết", 0)),
                                List.of(
                                        new ExamMatrixDTO.MatrixDetailRowDTO(
                                                "Chương",
                                                "Bài",
                                                "Đúng sai",
                                                "Nhận biết",
                                                0,
                                                "Dạng",
                                                "Yêu cầu"))));
    }

    private ExamQuestionDTO group(String type, String level, QuestionDetailResponseDTO question) {
        return new ExamQuestionDTO(type, level, List.of(question));
    }

    private QuestionDetailResponseDTO plainQuestion(String id) {
        return new QuestionDetailResponseDTO(
                id, new ContentDTO("Câu hỏi", new VariablesDTO(Map.of(), Map.of())), List.of());
    }
}
