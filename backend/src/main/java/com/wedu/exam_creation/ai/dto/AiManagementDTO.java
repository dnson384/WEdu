package com.wedu.exam_creation.ai.dto;

import lombok.Data;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import java.util.List;

public final class AiManagementDTO {
    private AiManagementDTO() { }

    @Data
    public static class BudgetDTO {
        @JsonDeserialize(using = IntegerDeserializer.class)
        private Long revision;
        private String monthlyBudgetUsd;
        @JsonDeserialize(using = IntegerDeserializer.class)
        private Long monthlyTokenQuota;
    }

    public static class IntegerDeserializer extends ValueDeserializer<Long> {
        @Override
        public Long deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.currentToken() != JsonToken.VALUE_NUMBER_INT) {
                return (Long) context.handleUnexpectedToken(Long.class, parser);
            }
            return parser.getLongValue();
        }
    }

    @Data
    public static class DailyUsageDTO {
        private String day;
        private long requests;
        private long unknownUsageRequests;
        private long inputTokens;
        private long outputTokens;
        private long totalTokens;
        private String estimatedCostUsd;
    }

    @Data
    public static class UsageReportDTO {
        private String from;
        private String to;
        private String model;
        private String timezone = "Asia/Ho_Chi_Minh";
        private String currency = "USD";
        private List<String> models;
        private DailyUsageDTO totals;
        private List<DailyUsageDTO> days;
    }

    @Data
    public static class BalanceInfoDTO {
        private String currency;
        private String totalBalance;
        private String grantedBalance;
        private String toppedUpBalance;
    }

    @Data
    public static class BalanceDTO {
        private Boolean available;
        private Instant updatedAt;
        private boolean stale;
        private List<BalanceInfoDTO> balances = List.of();
    }

    @Data
    public static class StatusDTO {
        private String month;
        private BudgetDTO budget;
        private DailyUsageDTO usage;
        private String remainingBudgetUsd;
        private Long remainingTokens;
        private Double budgetUsedPercent;
        private Double tokensUsedPercent;
        private String level;
        private List<String> reasons;
        private BalanceDTO balance;
    }
}
