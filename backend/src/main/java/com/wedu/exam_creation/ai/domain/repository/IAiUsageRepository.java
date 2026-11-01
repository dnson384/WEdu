package com.wedu.exam_creation.ai.domain.repository;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiDailyUsageEntity;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;

import java.time.Instant;
import java.util.List;

public interface IAiUsageRepository {
    void saveUsage(AiUsageEntity usage);

    List<AiDailyUsageEntity> findDaily(Instant from, Instant until, String model);

    List<String> findModels();

    AiBudgetEntity getBudget();

    AiBudgetEntity updateBudget(AiBudgetEntity budget);
}
