package com.fckedu.exam_creation.question.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamMatrixDetailDTO {
    String chapterId;
    String lessonId;
    String exerciseType;
    String difficultyLevel;
    String learningOutcome;
    String questionType;
    Integer limit;
}
