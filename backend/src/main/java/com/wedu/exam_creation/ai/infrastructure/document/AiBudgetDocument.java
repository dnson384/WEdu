package com.wedu.exam_creation.ai.infrastructure.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;

@Data
@Document("AiBudget")
public class AiBudgetDocument {
    @Id
    private String id;

    private long revision;

    @Field(targetType = FieldType.DECIMAL128)
    private BigDecimal monthlyBudgetUsd;

    private Long monthlyTokenQuota;
}
