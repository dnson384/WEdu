package com.fckedu.exam_creation.category.infrastructure.mapper;

import com.fckedu.exam_creation.category.domain.entity.CategoryEntity;
import com.fckedu.exam_creation.category.infrastructure.document.CategoryDocument;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    CategoryEntity toEntity(CategoryDocument document);

    CategoryDocument toDocument(CategoryEntity entity);
}
