package com.wedu.exam_creation.questionReview.infrastructure.mapper;

import com.wedu.exam_creation.questionReview.domain.entity.QuestionReviewEntity;
import com.wedu.exam_creation.questionReview.infrastructure.document.QuestionReviewDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface QuestionReviewMapper {
    QuestionReviewEntity toEntity(QuestionReviewDocument document);

    QuestionReviewDocument toDocument(QuestionReviewEntity entity);
}
