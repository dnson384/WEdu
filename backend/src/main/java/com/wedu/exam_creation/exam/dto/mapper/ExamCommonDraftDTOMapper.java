package com.wedu.exam_creation.exam.dto.mapper;

import com.wedu.exam_creation.common.dto.draft.response.LessonDraftDTO;
import com.wedu.exam_creation.exam.domain.entity.LessonExamEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ExamCommonDraftDTOMapper {
    LessonExamEntity lessonDraftDTOToLessonExamEntity(LessonDraftDTO dto);
}
