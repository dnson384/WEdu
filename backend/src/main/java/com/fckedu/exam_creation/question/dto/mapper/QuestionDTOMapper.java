package com.fckedu.exam_creation.question.dto.mapper;

import com.fckedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.fckedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.fckedu.exam_creation.question.domain.entity.QuestionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionDTOMapper {
    QuestionEntity newQuestionDTOToEntity(NewQuestionDTO dto);

    QuestionDTO entityToCommonDTO(QuestionEntity entity);
}
