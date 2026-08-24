package com.wedu.exam_creation.question.infrastructure.mapper;

import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import com.wedu.exam_creation.question.infrastructure.document.QuestionDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionMapper {
    QuestionEntity toEntity(QuestionDocument document);

    QuestionDocument toDocument(QuestionEntity entity);
}
