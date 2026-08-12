package com.fckedu.exam_creation.importer.dto.parsed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryImporterDTO {
    private String chapter;
    private List<LessonDataImporterDTO> lessons;
}
