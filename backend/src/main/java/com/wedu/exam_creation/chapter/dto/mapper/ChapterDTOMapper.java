package com.wedu.exam_creation.chapter.dto.mapper;

import com.wedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.wedu.exam_creation.chapter.domain.entity.LessonDataEntity;
import com.wedu.exam_creation.common.dto.chapter.NewLessonDataDTO;
import com.wedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.wedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChapterDTOMapper {
    ChapterResponseDTO entityToDTO(ChapterEntity entity);

    LessonDataEntity newLessonDTOToEntity(NewLessonDataDTO dto);

    LessonDataResponseDTO lessonEntityToDTO(LessonDataEntity dto);
}
