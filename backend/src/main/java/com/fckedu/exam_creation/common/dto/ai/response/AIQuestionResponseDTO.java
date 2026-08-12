package com.fckedu.exam_creation.common.dto.ai.response;

import com.fckedu.exam_creation.common.dto.question.response.ContentDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIQuestionResponseDTO {
    private String chapterId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private String learningOutcome;
    private String questionType;
    private ContentDTO question;
    private List<ContentDTO> options;
}
