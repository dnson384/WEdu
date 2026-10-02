package com.wedu.exam_creation.importer.dto.parsed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewChapterImporterDTO {
    private String name;
    private List<LessonDataImporterDTO> lessons;
}
