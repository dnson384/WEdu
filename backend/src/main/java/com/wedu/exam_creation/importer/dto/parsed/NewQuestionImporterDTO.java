package com.wedu.exam_creation.importer.dto.parsed;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewQuestionImporterDTO {
    @Builder.Default
    private String exerciseType = "";
    @Builder.Default
    private String difficultyLevel = "";
    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();
    @Builder.Default
    private String questionType = "";
    @Builder.Default
    private QuestionContentImporterDTO question = new QuestionContentImporterDTO();
    @Builder.Default
    private List<OptionsDataImporterDTO> options = new ArrayList<>();
}
