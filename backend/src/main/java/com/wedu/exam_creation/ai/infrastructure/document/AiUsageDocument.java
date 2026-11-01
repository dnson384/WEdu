package com.wedu.exam_creation.ai.infrastructure.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Document("AiUsage")
public class AiUsageDocument {
    @Id
    private String id;

    private Instant createdAt;
    private String model;
    private String outcome;
    private boolean usageKnown;
    private long inputTokens;
    private long outputTokens;
    private long cacheHitTokens;
    private long totalTokens;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal estimatedCostUsd;

    private String pricingVersion;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal inputCacheHitPrice;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal inputCacheMissPrice;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal outputPrice;
}
