package com.fckedu.exam_creation.draft.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixItemDocument {
    private String questionType;
    private String difficultyLevel;
    private Integer selectedCount;
}
