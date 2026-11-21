package com.wedu.exam_creation.ai.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.dto.ai.response.AIQuestionResponseDTO;
import com.wedu.exam_creation.common.exception.AIGenerationException;
import com.wedu.exam_creation.common.exception.AiAccessException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AIQuestionGenerationServiceTest {
    private final AiUsageService usage = mock(AiUsageService.class);
    private final AiUsageEntity event = new AiUsageEntity();
    private MockRestServiceServer server;
    private AIQuestionGenerationService service;

    @BeforeEach
    void setup() {
        DeepSeekProperties properties = new DeepSeekProperties();
        properties.setModel("deepseek-flash");
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        service =
                new AIQuestionGenerationService(
                        properties, new ObjectMapper(), usage, builder.build());
        when(usage.begin("deepseek-flash")).thenReturn(event);
    }

    private List<GenerateQuestionRequestDTO> requests() {
        GenerateQuestionRequestDTO input = new GenerateQuestionRequestDTO();
        input.setNumberOfQuestions(1);
        return List.of(input);
    }

    @Test
    @DisplayName(
            "trả về lỗi đọc JSON câu hỏi và vẫn ghi usage khi nội dung do AI trả về sai định dạng")
    void test_generateQuestions_001() {
        server.expect(requestTo("https://provider.invalid/chat/completions"))
                .andRespond(
                        withSuccess(
                                """
                {"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15},
                 "choices":[{"message":{"content":"invalid-json"}}]}
                """,
                                MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.generateQuestions(requests()))
                .isInstanceOf(JsonProcessingException.class);
        verify(usage)
                .complete(
                        eq(event),
                        argThat(root -> root.path("usage").path("total_tokens").asInt() == 15));
        server.verify();
    }

    @Test
    @DisplayName("trả về AiAccessException khi hết ngân sách và chưa gọi DeepSeek")
    void test_generateQuestions_002() {
        when(usage.begin(anyString()))
                .thenThrow(new AiAccessException("AI_BUDGET_EXCEEDED", 429, "limit"));
        assertThatThrownBy(() -> service.generateQuestions(requests()))
                .isInstanceOf(AiAccessException.class);
        verify(usage, never()).complete(any(), any());
        server.verify();
    }

    @Test
    @DisplayName(
            "trả về AIGenerationException và ghi nhận một lần gọi thất bại khi DeepSeek trả lỗi")
    void test_generateQuestions_003() {
        server.expect(requestTo("https://provider.invalid/chat/completions"))
                .andRespond(withServerError());
        assertThatThrownBy(() -> service.generateQuestions(requests()))
                .isInstanceOf(AIGenerationException.class);
        verify(usage).complete(event, null);
        server.verify();
    }

    @Test
    @DisplayName("trả về null khi danh sách yêu cầu trống và chưa ghi usage")
    void test_generateQuestions_004() throws Exception {
        assertThat(service.generateQuestions(List.of())).isNull();
        verifyNoInteractions(usage);
        server.verify();
    }

    @Test
    @DisplayName("trả về câu hỏi đã giải mã và ghi nhận usage khi DeepSeek trả phản hồi hợp lệ")
    void test_generateQuestions_005() throws JsonProcessingException {
        server.expect(requestTo("https://provider.invalid/chat/completions"))
                .andRespond(
                        withSuccess(
                                """
                {"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15},
                 "choices":[{"message":{"content":"{\\"questions\\":[{\\"chapterId\\":\\"chapter-1\\",\\"lessonId\\":\\"lesson-1\\"}]}"}}]}
                """,
                                MediaType.APPLICATION_JSON));

        List<AIQuestionResponseDTO> result = service.generateQuestions(requests());

        assertThat(result)
                .singleElement()
                .satisfies(
                        question -> {
                            assertThat(question.getChapterId()).isEqualTo("chapter-1");
                            assertThat(question.getLessonId()).isEqualTo("lesson-1");
                        });
        verify(usage)
                .complete(
                        eq(event),
                        argThat(root -> root.path("usage").path("total_tokens").asInt() == 15));
        server.verify();
    }

    @Test
    @DisplayName(
            "trả về AIGenerationException và ghi usage chưa xác định khi toàn bộ phản hồi DeepSeek sai định dạng")
    void test_generateQuestions_006() {
        server.expect(requestTo("https://provider.invalid/chat/completions"))
                .andRespond(withSuccess("invalid-json", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.generateQuestions(requests()))
                .isInstanceOf(AIGenerationException.class);

        verify(usage).complete(event, null);
        server.verify();
    }
}
