package com.wedu.exam_creation.exporter.usecase;

import com.wedu.exam_creation.common.dto.exam.response.ExamMatrixDTO;
import com.wedu.exam_creation.common.exception.InternalServerException;
import com.wedu.exam_creation.exam.usecase.ExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ExporterMatrixUsecaseTest {
    private final ExamService exams = mock(ExamService.class);
    private final ExporterUsecase usecase = new ExporterUsecase("http://exporter/exporter/", exams);
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(
            (RestTemplate) ReflectionTestUtils.getField(usecase, "restTemplate")).build();

    @BeforeEach
    void setup() {
        when(exams.getExamMatrixById("owner", "exam")).thenReturn(new ExamMatrixDTO("Đề tiếng Việt",
                List.of(new ExamMatrixDTO.MatrixRowDTO("Chương", "Bài", "Đúng sai", "Nhận biết", 0)),
                List.of(new ExamMatrixDTO.MatrixDetailRowDTO("Chương", "Bài", "Đúng sai", "Nhận biết", 0, "Dạng", "Yêu cầu"))));
    }

    @Test
    void postsSnapshotAndReturnsBinary() {
        server.expect(requestTo("http://exporter/exporter/matrix"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(jsonPath("$.examName").value("Đề tiếng Việt"))
                .andExpect(jsonPath("$.matrix[0].selectedCount").value(0))
                .andExpect(jsonPath("$.matrixDetails[0].learningOutcome").value("Yêu cầu"))
                .andRespond(withSuccess(new byte[]{80, 75, 3, 4}, MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document")));
        assertThat(usecase.exportMatrix("owner", "exam")).containsExactly(80, 75, 3, 4);
        server.verify();
        verify(exams).getExamMatrixById("owner", "exam");
        verifyNoMoreInteractions(exams);
    }

    @Test
    void rejectsJsonSuccessInsteadOfDocument() {
        server.expect(requestTo("http://exporter/exporter/matrix"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> usecase.exportMatrix("owner", "exam")).isInstanceOf(InternalServerException.class);
    }

    @Test
    void hidesExporterFailureDetails() {
        server.expect(requestTo("http://exporter/exporter/matrix")).andRespond(withServerError());
        assertThatThrownBy(() -> usecase.exportMatrix("owner", "exam"))
                .isInstanceOf(InternalServerException.class).hasMessageContaining("Vui lòng thử lại sau");
    }
}
