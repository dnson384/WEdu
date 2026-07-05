package com.fckedu.exam_creation.draft.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixDetailItemEntity {
    String exerciseType;
    String difficultyLevel;
    String learningOutcome;
    String questionType;
    Integer selectedCount;
}
