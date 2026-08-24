package com.wedu.exam_creation.question.dto.mapper;

import com.wedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.question.domain.entity.QuestionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionDTOMapper {
    QuestionEntity newQuestionDTOToEntity(NewQuestionDTO dto);

    QuestionDTO entityToCommonDTO(QuestionEntity entity);
}
