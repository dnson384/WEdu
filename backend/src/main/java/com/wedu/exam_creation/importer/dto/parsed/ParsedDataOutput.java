package com.wedu.exam_creation.importer.dto.parsed;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDataOutput {
    private List<NewQuestionImporterDTO> questions;
    private NewChapterImporterDTO chapter;
}