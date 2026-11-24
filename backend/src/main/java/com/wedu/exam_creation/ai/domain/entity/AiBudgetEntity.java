package com.wedu.exam_creation.ai.domain.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class AiBudgetEntity {
    private String id = "global";
    private long revision;
    private BigDecimal monthlyBudgetUsd;
    private Long monthlyTokenQuota;
}
