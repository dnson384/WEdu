package com.fckedu.exam_creation.chapter.infrastructure.mapper;

import com.fckedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.fckedu.exam_creation.chapter.infrastructure.document.ChapterDocument;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChapterMapper {
    ChapterEntity toEntity(ChapterDocument document);

    ChapterDocument toDocument(ChapterEntity entity);
}
