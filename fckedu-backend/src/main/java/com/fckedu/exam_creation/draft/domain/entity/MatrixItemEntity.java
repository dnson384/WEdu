package com.fckedu.exam_creation.draft.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixItemEntity {
    String questionType;
    String difficultyLevel;
    Integer selectedCount;
}
