package com.wedu.exam_creation.ai.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BalanceDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BudgetDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.StatusDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.UsageReportDTO;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import com.wedu.exam_creation.common.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class AiManagementUsecaseTest {
    private final IAiUsageRepository repository = mock(IAiUsageRepository.class);
    private final DeepSeekBalanceService balances = mock(DeepSeekBalanceService.class);
    private final DeepSeekProperties properties = new DeepSeekProperties();
    private AiManagementUsecase usecase;

    @BeforeEach
    void setup() {
        properties.setModel("deepseek-flash");
        usecase =
                new AiManagementUsecase(
                        repository,
                        properties,
                        balances,
                        Clock.fixed(Instant.parse("2026-10-31T18:00:00Z"), ZoneOffset.UTC));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of());
        when(repository.findModels()).thenReturn(List.of("historic-model"));
        when(repository.getBudget()).thenReturn(new AiBudgetEntity());
        BalanceDTO balance = new BalanceDTO();
        balance.setAvailable(true);
        when(balances.getBalance()).thenReturn(balance);
    }

    @Test
    @DisplayName("trả về báo cáo đủ ngày theo giờ Việt Nam khi vài ngày không có usage")
    void test_report_001() {
        AiDailyUsageEntity day = new AiDailyUsageEntity();
        day.setDay("2026-10-31");
        day.setRequests(2);
        day.setUnknownUsageRequests(1);
        day.setEstimatedCostUsd(new BigDecimal("0.12345678"));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        UsageReportDTO report = usecase.report("2026-10-30", "2026-11-01", "historic-model");
        verify(repository)
                .findDaily(
                        Instant.parse("2026-10-29T17:00:00Z"),
                        Instant.parse("2026-11-01T17:00:00Z"),
                        "historic-model");
        assertThat(report.getDays()).hasSize(3);
        assertThat(report.getDays().get(0).getRequests()).isZero();
        assertThat(report.getTotals().getEstimatedCostUsd()).isEqualTo("0.12345678");
        assertThat(report.getTotals().getUnknownUsageRequests()).isEqualTo(1);
        assertThat(report.getModels()).containsExactly("deepseek-flash", "historic-model");
    }

    @ParameterizedTest
    @CsvSource({"2026-11-02,2026-11-01", "invalid,2026-11-01", "2025-01-01,2026-01-02"})
    @DisplayName("trả về BadRequestException khi khoảng ngày sai định dạng, đảo ngược hoặc quá dài")
    void test_report_002(String from, String to) {
        assertThatThrownBy(() -> usecase.report(from, to, null))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).findDaily(any(), any(), any());
    }

    @Test
    @DisplayName("trả về báo cáo đủ 366 ngày khi khoảng ngày đạt giới hạn")
    void test_report_003() {
        assertThat(usecase.report("2024-01-01", "2024-12-31", null).getDays()).hasSize(366);
    }

    @ParameterizedTest
    @CsvSource({"79.99,NORMAL", "80,WARNING", "99.99,WARNING", "100,BLOCKED", "101,BLOCKED"})
    @DisplayName(
            "trả về mức cảnh báo và ngân sách còn lại khi chi phí ở quanh ngưỡng 80 hoặc 100 phần trăm")
    void test_status_004(String used, String expected) {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setMonthlyBudgetUsd(new BigDecimal("100"));
        when(repository.getBudget()).thenReturn(budget);
        AiDailyUsageEntity day = new AiDailyUsageEntity();
        day.setDay("2026-11-01");
        day.setEstimatedCostUsd(new BigDecimal(used));
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        StatusDTO result = usecase.status();
        assertThat(result.getLevel()).isEqualTo(expected);
        assertThat(new BigDecimal(result.getRemainingBudgetUsd()))
                .isEqualByComparingTo(
                        new BigDecimal("100").subtract(new BigDecimal(used)).max(BigDecimal.ZERO));
        if (expected.equals("BLOCKED"))
            assertThat(result.getReasons()).contains("AI_BUDGET_EXCEEDED");
    }

    @ParameterizedTest
    @CsvSource({"79,NORMAL", "80,WARNING", "99,WARNING", "100,BLOCKED", "110,BLOCKED"})
    @DisplayName(
            "trả về mức cảnh báo và quota còn lại khi số token ở quanh ngưỡng 80 hoặc 100 phần trăm")
    void test_status_005(long tokens, String expected) {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setMonthlyTokenQuota(100L);
        when(repository.getBudget()).thenReturn(budget);
        AiDailyUsageEntity day = new AiDailyUsageEntity();
        day.setDay("2026-11-01");
        day.setTotalTokens(tokens);
        when(repository.findDaily(any(), any(), any())).thenReturn(List.of(day));
        StatusDTO result = usecase.status();
        assertThat(result.getLevel()).isEqualTo(expected);
        assertThat(result.getRemainingTokens()).isEqualTo(Math.max(0, 100 - tokens));
    }

    @Test
    @DisplayName(
            "trả về trạng thái NORMAL của tháng mới khi sang ngày đầu tháng ở Việt Nam và chưa cấu hình hạn mức")
    void test_status_006() {
        StatusDTO result = usecase.status();
        assertThat(result.getMonth()).isEqualTo("2026-11");
        assertThat(result.getLevel()).isEqualTo("NORMAL");
        assertThat(result.getRemainingBudgetUsd()).isNull();
        assertThat(result.getRemainingTokens()).isNull();
        verify(repository)
                .findDaily(
                        Instant.parse("2026-10-31T17:00:00Z"),
                        Instant.parse("2026-11-01T17:00:00Z"),
                        null);
    }

    @Test
    @DisplayName(
            "trả về lý do chặn khi DeepSeek xác nhận hết số dư và trạng thái NORMAL khi snapshot chuyển sang stale")
    void test_status_007() {
        BalanceDTO balance = new BalanceDTO();
        balance.setAvailable(false);
        when(balances.getBalance()).thenReturn(balance);
        assertThat(usecase.status().getReasons()).containsExactly("AI_BALANCE_UNAVAILABLE");
        balance.setStale(true);
        assertThat(usecase.status().getLevel()).isEqualTo("NORMAL");
    }

    @ParameterizedTest
    @CsvSource({"0", "-1", "NaN", "1e2", "0.000000001", "1000000000000"})
    @DisplayName("trả về BadRequestException khi số tiền ngân sách không hợp lệ")
    void test_updateBudget_008(String value) {
        BudgetDTO input = new BudgetDTO();
        input.setRevision(0L);
        input.setMonthlyBudgetUsd(value);
        assertThatThrownBy(() -> usecase.updateBudget(input))
                .isInstanceOf(BadRequestException.class);
        verify(repository, never()).updateBudget(any());
    }

    @Test
    @DisplayName("trả về ngân sách đã lưu khi có số tiền hợp lệ và bỏ trống quota")
    void test_updateBudget_009() {
        BudgetDTO input = new BudgetDTO();
        input.setRevision(3L);
        input.setMonthlyBudgetUsd("0.00000001");
        when(repository.updateBudget(any())).thenAnswer(call -> call.getArgument(0));
        assertThat(usecase.updateBudget(input).getMonthlyBudgetUsd()).isEqualTo("0.00000001");
        ArgumentCaptor<AiBudgetEntity> capture = ArgumentCaptor.forClass(AiBudgetEntity.class);
        verify(repository).updateBudget(capture.capture());
        assertThat(capture.getValue().getRevision()).isEqualTo(3);
        assertThat(capture.getValue().getMonthlyTokenQuota()).isNull();
    }

    @Test
    @DisplayName("trả về ngân sách hiện hành khi kho dữ liệu có số tiền, quota và revision")
    void test_getBudget_010() {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setMonthlyBudgetUsd(new BigDecimal("10.2500"));
        budget.setMonthlyTokenQuota(1000L);
        budget.setRevision(3);
        when(repository.getBudget()).thenReturn(budget);

        BudgetDTO result = usecase.getBudget();

        assertThat(result.getMonthlyBudgetUsd()).isEqualTo("10.2500");
        assertThat(result.getMonthlyTokenQuota()).isEqualTo(1000L);
        assertThat(result.getRevision()).isEqualTo(3L);
    }

    @ParameterizedTest
    @CsvSource({"-1,1000", ",1000", "1,0", "1,9007199254740992"})
    @DisplayName("trả về BadRequestException khi revision bị thiếu, âm hoặc quota vượt giới hạn")
    void test_updateBudget_011(Long revision, Long quota) {
        BudgetDTO input = new BudgetDTO();
        input.setRevision(revision);
        input.setMonthlyTokenQuota(quota);

        assertThatThrownBy(() -> usecase.updateBudget(input))
                .isInstanceOf(BadRequestException.class);

        verify(repository, never()).updateBudget(any());
    }
}
