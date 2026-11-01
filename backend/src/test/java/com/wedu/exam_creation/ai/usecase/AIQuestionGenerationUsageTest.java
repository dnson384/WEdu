package com.wedu.exam_creation.ai.usecase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import com.wedu.exam_creation.common.dto.ai.request.GenerateQuestionRequestDTO;
import com.wedu.exam_creation.common.exception.AiAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AIQuestionGenerationUsageTest {
    private final AiUsageService usage = mock(AiUsageService.class);
    private final AiUsageEntity event = new AiUsageEntity();
    private MockRestServiceServer server;
    private AIQuestionGenerationService service;

    @BeforeEach
    void setup() {
        var properties = new DeepSeekProperties();
        properties.setModel("deepseek-flash");
        var builder = RestClient.builder().baseUrl("https://provider.invalid");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new AIQuestionGenerationService(properties, new ObjectMapper(), usage, builder.build());
        when(usage.begin("deepseek-flash")).thenReturn(event);
    }

    private List<GenerateQuestionRequestDTO> requests() {
        var input = new GenerateQuestionRequestDTO();
        input.setNumberOfQuestions(1);
        return List.of(input);
    }

    @Test
    void recordsProviderUsageEvenWhenQuestionJsonCannotBeParsed() {
        server.expect(requestTo("https://provider.invalid/chat/completions")).andRespond(withSuccess("""
                {"usage":{"prompt_tokens":10,"completion_tokens":5,"total_tokens":15},
                 "choices":[{"message":{"content":"invalid-json"}}]}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.generateQuestions(requests())).isInstanceOf(Exception.class);
        verify(usage).complete(eq(event), argThat(root -> root.path("usage").path("total_tokens").asInt() == 15));
        server.verify();
    }

    @Test
    void blockedCallNeverContactsProvider() {
        when(usage.begin(anyString())).thenThrow(new AiAccessException("AI_BUDGET_EXCEEDED", 429, "limit"));
        assertThatThrownBy(() -> service.generateQuestions(requests())).isInstanceOf(AiAccessException.class);
        verify(usage, never()).complete(any(), any());
        server.verify();
    }

    @Test
    void providerErrorIsRecordedOnceWithoutRetry() {
        server.expect(requestTo("https://provider.invalid/chat/completions")).andRespond(withServerError());
        assertThatThrownBy(() -> service.generateQuestions(requests())).isInstanceOf(Exception.class);
        verify(usage).complete(event, null);
        server.verify();
    }

    @Test
    void noRequestsDoesNotConsumeUsage() throws Exception {
        assertThat(service.generateQuestions(List.of())).isNull();
        verifyNoInteractions(usage);
    }
}
