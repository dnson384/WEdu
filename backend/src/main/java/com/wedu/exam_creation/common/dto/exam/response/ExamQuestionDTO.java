package com.wedu.exam_creation.common.dto.exam.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionDTO {
    private String questionType;
    private String difficultyLevel;
    private List<QuestionDetailResponseDTO> questions;
}
