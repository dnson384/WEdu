package com.fckedu.exam_creation.common.dto.draft.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatrixItemDTO {
    String questionType;
    String difficultyLevel;
    Integer selectedCount;
}
