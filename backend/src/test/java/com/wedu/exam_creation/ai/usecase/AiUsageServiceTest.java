package com.wedu.exam_creation.ai.usecase;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.StatusDTO;
import com.wedu.exam_creation.ai.properties.AiPricingProperties;
import com.wedu.exam_creation.common.exception.AiAccessException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AiUsageServiceTest {
    private final IAiUsageRepository repository = mock(IAiUsageRepository.class);
    private final AiManagementUsecase management = mock(AiManagementUsecase.class);
    private final AiPricingProperties pricing = new AiPricingProperties();
    private final AiUsageService service = new AiUsageService(repository, management, pricing);
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        StatusDTO status = new StatusDTO();
        status.setReasons(List.of());
        when(management.status()).thenReturn(status);
        AiPricingProperties.ModelPrice price = new AiPricingProperties.ModelPrice();
        price.setInputCacheHit(new BigDecimal("0.006"));
        price.setInputCacheMiss(new BigDecimal("0.3"));
        price.setOutput(new BigDecimal("1.2"));
        pricing.getModels().put("deepseek-flash", price);
        pricing.setVersion("test-peak");
    }

    @Test
    @DisplayName("trả về usage STARTED kèm bảng giá khi model đã được cấu hình")
    void test_begin_001() {
        AiUsageEntity usage = service.begin("deepseek-flash");
        assertThat(usage.getOutcome()).isEqualTo("STARTED");
        assertThat(usage.isUsageKnown()).isFalse();
        assertThat(usage.getPricingVersion()).isEqualTo("test-peak");
        assertThat(usage.getId()).isNotBlank();
        verify(repository).saveUsage(usage);
    }

    @Test
    @DisplayName(
            "trả về chi phí chính xác theo bảng giá lúc bắt đầu khi phản hồi có token cache hit và miss")
    void test_complete_002() throws Exception {
        AiUsageEntity usage = service.begin("deepseek-flash");
        // Later configuration changes must not reprice an in-flight call.
        pricing.getModels().get("deepseek-flash").setOutput(BigDecimal.TEN);
        service.complete(
                usage,
                json.readTree(
                        """
                {"usage":{"prompt_tokens":1000,"prompt_cache_hit_tokens":400,"completion_tokens":250,"total_tokens":1250}}
                """));
        assertThat(usage.isUsageKnown()).isTrue();
        assertThat(usage.getEstimatedCostUsd()).isEqualByComparingTo("0.0004824");
        assertThat(usage.getTotalTokens()).isEqualTo(1250);
        assertThat(usage.getCacheHitTokens()).isEqualTo(400);
        verify(repository, times(2)).saveUsage(usage);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"usage\":{}}",
                "{\"usage\":{\"prompt_tokens\":-1,\"completion_tokens\":2,\"total_tokens\":1}}",
                "{\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":9}}",
                "{\"usage\":{\"prompt_tokens\":1.5,\"completion_tokens\":2,\"total_tokens\":3}}"
            })
    @DisplayName(
            "trả về usage RECEIVED chưa biết chi phí khi dữ liệu token thiếu hoặc không hợp lệ")
    void test_complete_003(String body) throws Exception {
        AiUsageEntity usage = service.begin("deepseek-flash");
        service.complete(usage, json.readTree(body));
        assertThat(usage.isUsageKnown()).isFalse();
        assertThat(usage.getOutcome()).isEqualTo("RECEIVED");
        verify(repository, times(2)).saveUsage(usage);
    }

    @Test
    @DisplayName("trả về không có ngoại lệ khi lưu usage thất bại sau lỗi từ DeepSeek")
    void test_complete_004() {
        AiUsageEntity usage = service.begin("deepseek-flash");
        doThrow(new IllegalStateException("db unavailable")).when(repository).saveUsage(usage);
        assertThatCode(() -> service.complete(usage, null)).doesNotThrowAnyException();
        assertThat(usage.getOutcome()).isEqualTo("FAILED");
        assertThat(usage.isUsageKnown()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"AI_BUDGET_EXCEEDED", "AI_TOKEN_QUOTA_EXCEEDED", "AI_BALANCE_UNAVAILABLE"})
    @DisplayName("trả về AiAccessException 429 khi đạt giới hạn hoặc hết số dư")
    void test_begin_005(String reason) {
        StatusDTO status = new StatusDTO();
        status.setReasons(List.of(reason));
        when(management.status()).thenReturn(status);
        assertThatThrownBy(() -> service.begin("deepseek-flash"))
                .isInstanceOfSatisfying(
                        AiAccessException.class,
                        error -> {
                            assertThat(error.getCode()).isEqualTo(reason);
                            assertThat(error.getStatus()).isEqualTo(429);
                        });
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("trả về AI_TRACKING_UNAVAILABLE khi không đọc được hạn mức")
    void test_begin_006() {
        when(management.status()).thenThrow(new IllegalStateException("db unavailable"));
        assertThatThrownBy(() -> service.begin("deepseek-flash"))
                .isInstanceOfSatisfying(
                        AiAccessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("AI_TRACKING_UNAVAILABLE"));
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("trả về AiAccessException 503 khi không lưu được usage STARTED")
    void test_begin_007() {
        doThrow(new IllegalStateException("db unavailable"))
                .when(repository)
                .saveUsage(any(AiUsageEntity.class));
        assertThatThrownBy(() -> service.begin("deepseek-flash"))
                .isInstanceOfSatisfying(
                        AiAccessException.class,
                        error -> assertThat(error.getStatus()).isEqualTo(503));
    }

    @Test
    @DisplayName("trả về AI_PRICING_UNAVAILABLE khi model thiếu bảng giá")
    void test_begin_008() {
        assertThatThrownBy(() -> service.begin("unpriced-model"))
                .isInstanceOfSatisfying(
                        AiAccessException.class,
                        error -> assertThat(error.getCode()).isEqualTo("AI_PRICING_UNAVAILABLE"));
        verifyNoInteractions(repository);
    }
}
