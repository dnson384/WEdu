package com.fckedu.exam_creation.draft.domain.payload;

import com.fckedu.exam_creation.draft.domain.entity.MatrixItemEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatrixPayload {
    private String draftId;
    private String userId;
    private String chapterId;
    private String lessonId;
    private List<MatrixItemEntity> matrix;
}
