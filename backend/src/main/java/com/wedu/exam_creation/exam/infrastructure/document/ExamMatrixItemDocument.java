package com.wedu.exam_creation.exam.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixItemDocument {
    private String questionType;
    private String difficultyLevel;
    private Integer selectedCount;
}
