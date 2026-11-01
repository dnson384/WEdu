package com.wedu.exam_creation.ai.usecase;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BalanceDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BudgetDTO;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import com.wedu.exam_creation.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AiManagementUsecaseTest {
    private final IAiUsageRepository repository = mock(IAiUsageRepository.class);
    private final DeepSeekBalanceService balances = mock(DeepSeekBalanceService.class);
    private final DeepSeekProperties properties = new DeepSeekProperties();
    private AiManagementUsecase usecase;

    @BeforeEach
    void setup() {
        properties.setModel("deepseek-flash");
        usecase = new AiManagementUsecase(repository, properties, balances,
                Clock.fixed(Instant.parse("2026-10-31T18:00:00Z"), ZoneOffset.UTC));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of());
        when(repository.findModels()).thenReturn(List.of("historic-model"));
        when(repository.getBudget()).thenReturn(new AiBudgetEntity());
        var balance = new BalanceDTO();
        balance.setAvailable(true);
        when(balances.getBalance()).thenReturn(balance);
    }

    @Test
    void inclusiveDatesAreConvertedToVietnamMidnightsAndEmptyDaysAreFilled() {
        var day = new AiDailyUsageEntity();
        day.setDay("2026-10-31");
        day.setRequests(2);
        day.setUnknownUsageRequests(1);
        day.setEstimatedCostUsd(new BigDecimal("0.12345678"));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        var report = usecase.report("2026-10-30", "2026-11-01", "historic-model");
        verify(repository).findDaily(Instant.parse("2026-10-29T17:00:00Z"), Instant.parse("2026-11-01T17:00:00Z"), "historic-model");
        assertThat(report.getDays()).hasSize(3);
        assertThat(report.getDays().get(0).getRequests()).isZero();
        assertThat(report.getTotals().getEstimatedCostUsd()).isEqualTo("0.12345678");
        assertThat(report.getTotals().getUnknownUsageRequests()).isEqualTo(1);
        assertThat(report.getModels()).containsExactly("deepseek-flash", "historic-model");
    }

    @ParameterizedTest
    @CsvSource({"2026-11-02,2026-11-01", "invalid,2026-11-01", "2025-01-01,2026-01-02"})
    void rejectsInvalidDateRanges(String from, String to) {
        assertThatThrownBy(() -> usecase.report(from, to, null)).isInstanceOf(BadRequestException.class);
        verify(repository, never()).findDaily(any(), any(), any());
    }

    @Test
    void allowsExactly366Days() {
        assertThat(usecase.report("2024-01-01", "2024-12-31", null).getDays()).hasSize(366);
    }

    @ParameterizedTest
    @CsvSource({"79.99,NORMAL", "80,WARNING", "99.99,WARNING", "100,BLOCKED", "101,BLOCKED"})
    void evaluatesMoneyThresholdsExactly(String used, String expected) {
        var budget = new AiBudgetEntity();
        budget.setMonthlyBudgetUsd(new BigDecimal("100"));
        when(repository.getBudget()).thenReturn(budget);
        var day = new AiDailyUsageEntity();
        day.setDay("2026-11-01");
        day.setEstimatedCostUsd(new BigDecimal(used));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        var result = usecase.status();
        assertThat(result.getLevel()).isEqualTo(expected);
        assertThat(new BigDecimal(result.getRemainingBudgetUsd())).isEqualByComparingTo(new BigDecimal("100").subtract(new BigDecimal(used)).max(BigDecimal.ZERO));
        if (expected.equals("BLOCKED")) assertThat(result.getReasons()).contains("AI_BUDGET_EXCEEDED");
    }

    @ParameterizedTest
    @CsvSource({"79,NORMAL", "80,WARNING", "99,WARNING", "100,BLOCKED", "110,BLOCKED"})
    void evaluatesTokenThresholds(long tokens, String expected) {
        var budget = new AiBudgetEntity();
        budget.setMonthlyTokenQuota(100L);
        when(repository.getBudget()).thenReturn(budget);
        var day = new AiDailyUsageEntity();
        day.setDay("2026-11-01");
        day.setTotalTokens(tokens);
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        var result = usecase.status();
        assertThat(result.getLevel()).isEqualTo(expected);
        assertThat(result.getRemainingTokens()).isEqualTo(Math.max(0, 100 - tokens));
    }

    @Test
    void monthResetUsesVietnamTimeAndLeavesUnconfiguredLimitsDisabled() {
        var result = usecase.status();
        assertThat(result.getMonth()).isEqualTo("2026-11");
        assertThat(result.getLevel()).isEqualTo("NORMAL");
        assertThat(result.getRemainingBudgetUsd()).isNull();
        assertThat(result.getRemainingTokens()).isNull();
        verify(repository).findDaily(Instant.parse("2026-10-31T17:00:00Z"), Instant.parse("2026-11-01T17:00:00Z"), null);
    }

    @Test
    void confirmedEmptyBalanceBlocksButStaleSnapshotDoesNot() {
        var balance = new BalanceDTO();
        balance.setAvailable(false);
        when(balances.getBalance()).thenReturn(balance);
        assertThat(usecase.status().getReasons()).containsExactly("AI_BALANCE_UNAVAILABLE");
        balance.setStale(true);
        assertThat(usecase.status().getLevel()).isEqualTo("NORMAL");
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "NaN", "1e2", "0.000000001", "1000000000000"})
    void rejectsInvalidBudgetAmounts(String value) {
        var input = new BudgetDTO();
        input.setRevision(0L);
        input.setMonthlyBudgetUsd(value);
        assertThatThrownBy(() -> usecase.updateBudget(input)).isInstanceOf(BadRequestException.class);
        verify(repository, never()).updateBudget(any());
    }

    @Test
    void savesOptionalLimitsWithOriginalRevision() {
        var input = new BudgetDTO();
        input.setRevision(3L);
        input.setMonthlyBudgetUsd("0.00000001");
        when(repository.updateBudget(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(usecase.updateBudget(input).getMonthlyBudgetUsd()).isEqualTo("0.00000001");
        var capture = ArgumentCaptor.forClass(AiBudgetEntity.class);
        verify(repository).updateBudget(capture.capture());
        assertThat(capture.getValue().getRevision()).isEqualTo(3);
        assertThat(capture.getValue().getMonthlyTokenQuota()).isNull();
    }
}
