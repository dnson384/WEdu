package com.wedu.exam_creation.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.StatusDTO;
import com.wedu.exam_creation.ai.properties.AiPricingProperties;
import com.wedu.exam_creation.ai.usecase.AiManagementUsecase;
import com.wedu.exam_creation.ai.usecase.AiUsageService;
import com.wedu.exam_creation.common.exception.AiAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiUsageServiceTest {
    private final IAiUsageRepository repository = mock(IAiUsageRepository.class);
    private final AiManagementUsecase management = mock(AiManagementUsecase.class);
    private final AiPricingProperties pricing = new AiPricingProperties();
    private final AiUsageService service = new AiUsageService(repository, management, pricing);
    private final ObjectMapper json = new ObjectMapper();

    @BeforeEach
    void setup() {
        var status = new StatusDTO();
        status.setReasons(List.of());
        when(management.status()).thenReturn(status);
        var price = new AiPricingProperties.ModelPrice();
        price.setInputCacheHit(new BigDecimal("0.006"));
        price.setInputCacheMiss(new BigDecimal("0.3"));
        price.setOutput(new BigDecimal("1.2"));
        pricing.getModels().put("deepseek-flash", price);
        pricing.setVersion("test-peak");
    }

    @Test
    void persistsStartedRecordBeforeCallingProviderAndSnapshotsPrices() {
        var usage = service.begin("deepseek-flash");
        assertThat(usage.getOutcome()).isEqualTo("STARTED");
        assertThat(usage.isUsageKnown()).isFalse();
        assertThat(usage.getPricingVersion()).isEqualTo("test-peak");
        assertThat(usage.getId()).isNotBlank();
        verify(repository).saveUsage(usage);
    }

    @Test
    void calculatesCacheHitMissAndOutputWithDecimalPrecision() throws Exception {
        var usage = service.begin("deepseek-flash");
        // Later configuration changes must not reprice an in-flight call.
        pricing.getModels().get("deepseek-flash").setOutput(BigDecimal.TEN);
        service.complete(usage, json.readTree("""
                {"usage":{"prompt_tokens":1000,"prompt_cache_hit_tokens":400,"completion_tokens":250,"total_tokens":1250}}
                """));
        assertThat(usage.isUsageKnown()).isTrue();
        assertThat(usage.getEstimatedCostUsd()).isEqualByComparingTo("0.0004824");
        assertThat(usage.getTotalTokens()).isEqualTo(1250);
        assertThat(usage.getCacheHitTokens()).isEqualTo(400);
        verify(repository, times(2)).saveUsage(usage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"usage\":{}}", "{\"usage\":{\"prompt_tokens\":-1,\"completion_tokens\":2,\"total_tokens\":1}}",
            "{\"usage\":{\"prompt_tokens\":1,\"completion_tokens\":2,\"total_tokens\":9}}",
            "{\"usage\":{\"prompt_tokens\":1.5,\"completion_tokens\":2,\"total_tokens\":3}}"})
    void incompleteOrInvalidUsageRemainsExplicitlyUnknown(String body) throws Exception {
        var usage = service.begin("deepseek-flash");
        service.complete(usage, json.readTree(body));
        assertThat(usage.isUsageKnown()).isFalse();
        assertThat(usage.getOutcome()).isEqualTo("RECEIVED");
        verify(repository, times(2)).saveUsage(usage);
    }

    @Test
    void failureIsUnknownAndRecordingFailureDoesNotRetryBillableWork() {
        var usage = service.begin("deepseek-flash");
        doThrow(new IllegalStateException("db unavailable")).when(repository).saveUsage(usage);
        assertThatCode(() -> service.complete(usage, null)).doesNotThrowAnyException();
        assertThat(usage.getOutcome()).isEqualTo("FAILED");
        assertThat(usage.isUsageKnown()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"AI_BUDGET_EXCEEDED", "AI_TOKEN_QUOTA_EXCEEDED", "AI_BALANCE_UNAVAILABLE"})
    void blocksBeforeCreatingUsageWhenLimitReached(String reason) {
        var status = new StatusDTO();
        status.setReasons(List.of(reason));
        when(management.status()).thenReturn(status);
        assertThatThrownBy(() -> service.begin("deepseek-flash")).isInstanceOfSatisfying(AiAccessException.class, error -> {
            assertThat(error.getCode()).isEqualTo(reason);
            assertThat(error.getStatus()).isEqualTo(429);
        });
        verifyNoInteractions(repository);
    }

    @Test
    void failsClosedOnDatabaseFailure() {
        when(management.status()).thenThrow(new IllegalStateException("db unavailable"));
        assertThatThrownBy(() -> service.begin("deepseek-flash")).isInstanceOfSatisfying(AiAccessException.class,
                error -> assertThat(error.getCode()).isEqualTo("AI_TRACKING_UNAVAILABLE"));
        verifyNoInteractions(repository);
    }

    @Test
    void failsClosedBeforeProviderWhenStartedRecordCannotBeWritten() {
        doThrow(new IllegalStateException("db unavailable")).when(repository).saveUsage(any(AiUsageEntity.class));
        assertThatThrownBy(() -> service.begin("deepseek-flash")).isInstanceOfSatisfying(AiAccessException.class,
                error -> assertThat(error.getStatus()).isEqualTo(503));
    }

    @Test
    void unknownModelRequiresExplicitPricing() {
        assertThatThrownBy(() -> service.begin("unpriced-model")).isInstanceOfSatisfying(AiAccessException.class,
                error -> assertThat(error.getCode()).isEqualTo("AI_PRICING_UNAVAILABLE"));
        verifyNoInteractions(repository);
    }
}
