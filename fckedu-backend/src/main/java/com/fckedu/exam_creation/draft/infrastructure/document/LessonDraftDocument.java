package com.fckedu.exam_creation.draft.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDraftDocument {
    @Builder.Default
    private String id = (new ObjectId()).toString();

    private String name;

    @Builder.Default
    private List<MatrixItemDocument> matrix = new ArrayList<>();

    @Builder.Default
    private List<MatrixDetailItemDocument> matrixDetails = new ArrayList<>();
}
