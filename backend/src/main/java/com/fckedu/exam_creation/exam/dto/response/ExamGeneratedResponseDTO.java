package com.fckedu.exam_creation.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamGeneratedResponseDTO {
    private String examId;
    private List<String> errors;
}
