package com.wedu.exam_creation.question.dto.mapper;

import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import org.mapstruct.Mapper;
import org.mapstruct.BeanMapping;
import org.mapstruct.Builder;
import org.mapstruct.Mapping;
import com.wedu.exam_creation.common.dto.question.response.PendingQuestionDTO;

@Mapper(componentModel = "spring")
public interface QuestionDTOMapper {
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "chapter", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    PendingQuestionDTO entityToPendingDTO(QuestionEntity entity);

    QuestionEntity newQuestionDTOToEntity(NewQuestionDTO dto);

    QuestionDTO entityToCommonDTO(QuestionEntity entity);
}
