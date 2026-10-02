package com.wedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixDetailItemEntity {
    String exerciseType;
    String difficultyLevel;
    String learningOutcome;
    String questionType;
    Integer selectedCount;
}
