package com.wedu.exam_creation.common.dto.exam.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamGeneratedDTO {
    private List<ExamQuestionGeneratedDTO> questions;
    private List<String> errors;
}
