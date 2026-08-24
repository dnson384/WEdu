package com.wedu.exam_creation.exam.infrastructure.mapper;

import com.wedu.exam_creation.exam.domain.entity.ExamEntity;
import com.wedu.exam_creation.exam.infrastructure.document.ExamDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExamMapper {
    ExamEntity docToEntity(ExamDocument document);

    ExamDocument entityToDocument(ExamEntity entity);
}
