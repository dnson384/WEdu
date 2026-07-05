package com.fckedu.exam_creation.category.dto.mapper;

import com.fckedu.exam_creation.category.domain.entity.CategoryEntity;
import com.fckedu.exam_creation.category.domain.entity.LessonDataEntity;
import com.fckedu.exam_creation.common.dto.category.NewLessonDataDTO;
import com.fckedu.exam_creation.common.dto.category.response.CategoryResponseDTO;
import com.fckedu.exam_creation.common.dto.category.response.LessonDataResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryDTOMapper {
    CategoryResponseDTO entityToDTO(CategoryEntity entity);

    CategoryEntity dtoToEntity(CategoryResponseDTO dto);

    LessonDataEntity newLessonDTOToEntity(NewLessonDataDTO dto);

    LessonDataResponseDTO lessonEntityToDTO(LessonDataEntity dto);

}
