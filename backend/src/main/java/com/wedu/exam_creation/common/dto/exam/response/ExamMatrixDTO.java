package com.wedu.exam_creation.common.dto.exam.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixDTO {
    private String examName;
    private List<MatrixRowDTO> matrix;
    private List<MatrixDetailRowDTO> matrixDetails;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixRowDTO {
        private String chapterName;
        private String lessonName;
        private String questionType;
        private String difficultyLevel;
        private Integer selectedCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MatrixDetailRowDTO {
        private String chapterName;
        private String lessonName;
        private String questionType;
        private String difficultyLevel;
        private Integer selectedCount;
        private String exerciseType;
        private String learningOutcome;
    }
}
