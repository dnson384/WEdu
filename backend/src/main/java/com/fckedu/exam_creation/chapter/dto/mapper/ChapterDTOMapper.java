package com.fckedu.exam_creation.chapter.dto.mapper;

import com.fckedu.exam_creation.chapter.domain.entity.ChapterEntity;
import com.fckedu.exam_creation.chapter.domain.entity.LessonDataEntity;
import com.fckedu.exam_creation.common.dto.chapter.NewLessonDataDTO;
import com.fckedu.exam_creation.common.dto.chapter.response.ChapterResponseDTO;
import com.fckedu.exam_creation.common.dto.chapter.response.LessonDataResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ChapterDTOMapper {
    ChapterResponseDTO entityToDTO(ChapterEntity entity);

    ChapterEntity dtoToEntity(ChapterResponseDTO dto);

    LessonDataEntity newLessonDTOToEntity(NewLessonDataDTO dto);

    LessonDataResponseDTO lessonEntityToDTO(LessonDataEntity dto);

}
