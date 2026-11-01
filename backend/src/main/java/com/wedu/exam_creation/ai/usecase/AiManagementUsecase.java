package com.wedu.exam_creation.ai.usecase;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.repository.IAiUsageRepository;
import com.wedu.exam_creation.ai.dto.AiManagementDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.BudgetDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.DailyUsageDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.StatusDTO;
import com.wedu.exam_creation.ai.dto.AiManagementDTO.UsageReportDTO;
import com.wedu.exam_creation.ai.properties.DeepSeekProperties;
import com.wedu.exam_creation.common.exception.BadRequestException;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class AiManagementUsecase {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final IAiUsageRepository repository;
    private final DeepSeekProperties properties;
    private final DeepSeekBalanceService balanceService;
    private final Clock clock;

    @Autowired
    public AiManagementUsecase(IAiUsageRepository repository, DeepSeekProperties properties, DeepSeekBalanceService balanceService) {
        this(repository, properties, balanceService, Clock.systemUTC());
    }

    AiManagementUsecase(IAiUsageRepository repository, DeepSeekProperties properties, DeepSeekBalanceService balanceService, Clock clock) {
        this.repository = repository;
        this.properties = properties;
        this.balanceService = balanceService;
        this.clock = clock;
    }

    private static @NonNull AiBudgetEntity getAiBudgetEntity(BudgetDTO input) {
        AiBudgetEntity budget = new AiBudgetEntity();
        budget.setRevision(input.getRevision());
        budget.setMonthlyTokenQuota(input.getMonthlyTokenQuota());

        if (input.getMonthlyBudgetUsd() != null) {
            String amount = input.getMonthlyBudgetUsd();
            if (!amount.matches("[0-9]{1,12}(\\.[0-9]{1,8})?")) {
                throw new BadRequestException("Budget USD phải là số dương, tối đa 8 số thập phân");
            }

            BigDecimal value = new BigDecimal(amount);
            if (value.signum() <= 0) {
                throw new BadRequestException("Budget USD phải lớn hơn 0");
            }

            budget.setMonthlyBudgetUsd(value);
        }
        return budget;
    }

    public UsageReportDTO report(String from, String to, String model) {
        LocalDate start;
        LocalDate end;

        try {
            start = LocalDate.parse(from);
            end = LocalDate.parse(to);
        } catch (Exception invalid) {
            throw new BadRequestException("Ngày lọc phải có định dạng YYYY-MM-DD");
        }

        if (end.isBefore(start) || ChronoUnit.DAYS.between(start, end) >= 366) {
            throw new BadRequestException("Khoảng lọc phải từ 1 đến 366 ngày");
        }

        if (model != null && (model.isBlank() || model.length() > 100)) {
            throw new BadRequestException("Model không hợp lệ");
        }

        List<AiDailyUsageEntity> entries = repository.findDaily(
                start.atStartOfDay(ZONE).toInstant(),
                end.plusDays(1).atStartOfDay(ZONE).toInstant(), model);

        Map<String, AiDailyUsageEntity> byDay = new HashMap<>();
        entries.forEach(item -> byDay.put(item.getDay(), item));

        List<DailyUsageDTO> days = new ArrayList<>();
        AiDailyUsageEntity total = new AiDailyUsageEntity();
        for (LocalDate day = start; !day.isAfter(end); day = day.plusDays(1)) {
            AiDailyUsageEntity entry = byDay.getOrDefault(day.toString(), new AiDailyUsageEntity());
            entry.setDay(day.toString());

            days.add(toDTO(entry));

            total.setRequests(total.getRequests() + entry.getRequests());
            total.setUnknownUsageRequests(total.getUnknownUsageRequests() + entry.getUnknownUsageRequests());
            total.setInputTokens(total.getInputTokens() + entry.getInputTokens());
            total.setOutputTokens(total.getOutputTokens() + entry.getOutputTokens());
            total.setTotalTokens(total.getTotalTokens() + entry.getTotalTokens());
            total.setEstimatedCostUsd(total.getEstimatedCostUsd().add(entry.getEstimatedCostUsd()));
        }

        Set<String> models = new TreeSet<>(repository.findModels());
        models.add(properties.getModel());

        UsageReportDTO result = new UsageReportDTO();
        result.setFrom(from);
        result.setTo(to);
        result.setModel(model);
        result.setModels(new ArrayList<>(models));
        result.setTotals(toDTO(total));
        result.setDays(days);

        return result;
    }

    public BudgetDTO getBudget() {
        return toDTO(repository.getBudget());
    }

    public BudgetDTO updateBudget(BudgetDTO input) {
        if (input == null ||
                input.getRevision() == null ||
                input.getRevision() < 0 ||
                (input.getMonthlyTokenQuota() != null &&
                        (input.getMonthlyTokenQuota() <= 0 ||
                                input.getMonthlyTokenQuota() > 9007199254740991L))
        ) {
            throw new BadRequestException("Hạn mức hoặc phiên bản cấu hình không hợp lệ");
        }

        AiBudgetEntity budget = getAiBudgetEntity(input);

        return toDTO(repository.updateBudget(budget));
    }

    public StatusDTO status() {
        LocalDate today = LocalDate.now(clock.withZone(ZONE));
        AiManagementDTO.UsageReportDTO report = report(today.withDayOfMonth(1).toString(), today.toString(), null);
        BudgetDTO budget = getBudget();
        DailyUsageDTO usage = report.getTotals();

        List<String> reasons = new ArrayList<>();
        StatusDTO status = new StatusDTO();
        status.setMonth(YearMonth.from(today).toString());
        status.setBudget(budget);
        status.setUsage(usage);
        boolean warning = false;

        if (budget.getMonthlyBudgetUsd() != null) {
            BigDecimal limit = new BigDecimal(budget.getMonthlyBudgetUsd());
            BigDecimal used = new BigDecimal(usage.getEstimatedCostUsd());
            status.setRemainingBudgetUsd(limit.subtract(used).max(BigDecimal.ZERO).toPlainString());
            status.setBudgetUsedPercent(percent(used, limit));
            warning = used.compareTo(limit.multiply(new BigDecimal("0.8"))) >= 0;

            if (used.compareTo(limit) >= 0) {
                reasons.add("AI_BUDGET_EXCEEDED");
            }
        }

        if (budget.getMonthlyTokenQuota() != null) {
            long limit = budget.getMonthlyTokenQuota();
            status.setRemainingTokens(Math.max(0, limit - usage.getTotalTokens()));
            status.setTokensUsedPercent(percent(BigDecimal.valueOf(usage.getTotalTokens()), BigDecimal.valueOf(limit)));

            warning |= BigDecimal.valueOf(usage.getTotalTokens())
                    .compareTo(BigDecimal.valueOf(limit).multiply(new BigDecimal("0.8"))) >= 0;

            if (usage.getTotalTokens() >= limit) {
                reasons.add("AI_TOKEN_QUOTA_EXCEEDED");
            }
        }

        AiManagementDTO.BalanceDTO balance = balanceService.getBalance();
        if (!balance.isStale() && Boolean.FALSE.equals(balance.getAvailable())) {
            reasons.add("AI_BALANCE_UNAVAILABLE");
        }

        status.setBalance(balance);
        status.setReasons(reasons);
        status.setLevel(!reasons.isEmpty() ? "BLOCKED" : warning ? "WARNING" : "NORMAL");

        return status;
    }

    private double percent(BigDecimal used, BigDecimal limit) {
        return used.multiply(BigDecimal.valueOf(100)).divide(limit, 2, RoundingMode.HALF_UP).doubleValue();
    }

    private BudgetDTO toDTO(AiBudgetEntity entity) {
        BudgetDTO dto = new BudgetDTO();
        dto.setRevision(entity.getRevision());
        dto.setMonthlyBudgetUsd(entity.getMonthlyBudgetUsd() == null ? null : entity.getMonthlyBudgetUsd().toPlainString());
        dto.setMonthlyTokenQuota(entity.getMonthlyTokenQuota());
        return dto;
    }

    private DailyUsageDTO toDTO(AiDailyUsageEntity entity) {
        DailyUsageDTO dto = new DailyUsageDTO();
        dto.setDay(entity.getDay());
        dto.setRequests(entity.getRequests());
        dto.setUnknownUsageRequests(entity.getUnknownUsageRequests());
        dto.setInputTokens(entity.getInputTokens());
        dto.setOutputTokens(entity.getOutputTokens());
        dto.setTotalTokens(entity.getTotalTokens());
        dto.setEstimatedCostUsd(entity.getEstimatedCostUsd().toPlainString());
        return dto;
    }
}
