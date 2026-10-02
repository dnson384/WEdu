package com.wedu.exam_creation.exam.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixDetailItemDocument {
    private String exerciseType;
    private String difficultyLevel;
    private String learningOutcome;
    private String questionType;
    private Integer selectedCount;
}
