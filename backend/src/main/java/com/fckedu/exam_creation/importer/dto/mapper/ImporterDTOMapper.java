package com.fckedu.exam_creation.importer.dto.mapper;

import com.fckedu.exam_creation.common.dto.question.NewQuestionDTO;
import com.fckedu.exam_creation.importer.dto.parsed.NewQuestionImporterDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ImporterDTOMapper {
    NewQuestionDTO mapQuestionToDTO(NewQuestionImporterDTO newQuestionImporterDTO);
}
