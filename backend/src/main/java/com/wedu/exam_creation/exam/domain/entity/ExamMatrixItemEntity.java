package com.wedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixItemEntity {
    String questionType;
    String difficultyLevel;
    Integer selectedCount;
}
