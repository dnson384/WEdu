package com.wedu.exam_creation.questionReview.infrastructure.mapper;

import com.wedu.exam_creation.common.dto.question.response.QuestionDTO;
import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.domain.entity.RegeneratedQuestionEntity;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionReviewMapper {
    QuestionDTO toDTO(RegeneratedQuestionEntity entity);

    RegeneratedQuestionEntity toSnapshot(QuestionDTO dto);

    QuestionReviewEntity toEntity(QuestionReviewDocument document);

    QuestionReviewDocument toDocument(QuestionReviewEntity entity);
}
