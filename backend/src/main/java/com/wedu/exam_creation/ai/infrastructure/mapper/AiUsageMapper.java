package com.wedu.exam_creation.ai.infrastructure.mapper;

import com.wedu.exam_creation.ai.domain.entity.AiBudgetEntity;
import com.wedu.exam_creation.ai.domain.entity.AiUsageEntity;
import com.wedu.exam_creation.ai.infrastructure.document.AiBudgetDocument;
import com.wedu.exam_creation.ai.infrastructure.document.AiUsageDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiUsageMapper {
    AiUsageDocument toDocument(AiUsageEntity entity);

    AiBudgetEntity toEntity(AiBudgetDocument document);

    AiBudgetDocument toDocument(AiBudgetEntity entity);
}
