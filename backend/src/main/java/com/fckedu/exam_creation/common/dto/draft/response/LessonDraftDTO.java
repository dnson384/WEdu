package com.fckedu.exam_creation.common.dto.draft.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonDraftDTO {
    String id;
    String name;
    List<MatrixItemDTO> matrix;
    List<MatrixDetailItemDTO> matrixDetails;
}
