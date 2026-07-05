package com.fckedu.exam_creation.common.dto.exam.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionGeneratedDTO {
    String questionType;
    String difficultyLevel;
    List<String> questionIds;
}
