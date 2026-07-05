package com.fckedu.exam_creation.draft.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixDetailItemDocument {
    private String exerciseType;
    private String difficultyLevel;
    private String learningOutcome;
    private String questionType;
    private Integer selectedCount;
}
