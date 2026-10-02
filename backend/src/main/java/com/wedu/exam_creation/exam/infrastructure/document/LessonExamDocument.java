package com.wedu.exam_creation.exam.infrastructure.document;

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
public class LessonExamDocument {
    @Builder.Default
    private String id = (new ObjectId()).toString();

    private String name;

    @Builder.Default
    private List<ExamMatrixItemDocument> matrix = new ArrayList<>();

    @Builder.Default
    private List<ExamMatrixDetailItemDocument> matrixDetails = new ArrayList<>();
}
