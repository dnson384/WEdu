package com.wedu.exam_creation.ai.domain.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AiDailyUsageEntity {
    private String day;
    private long requests;
    private long unknownUsageRequests;
    private long inputTokens;
    private long outputTokens;
    private long totalTokens;
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;
}
