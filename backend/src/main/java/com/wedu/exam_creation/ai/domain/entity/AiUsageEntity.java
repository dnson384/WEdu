package com.wedu.exam_creation.ai.domain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class AiUsageEntity {
    private String id;
    private Instant createdAt;
    private String model;
    private String outcome;
    private boolean usageKnown;
    private long inputTokens;
    private long outputTokens;
    private long cacheHitTokens;
    private long totalTokens;
    private BigDecimal estimatedCostUsd = BigDecimal.ZERO;
    private String pricingVersion;
    private BigDecimal inputCacheHitPrice;
    private BigDecimal inputCacheMissPrice;
    private BigDecimal outputPrice;
}
