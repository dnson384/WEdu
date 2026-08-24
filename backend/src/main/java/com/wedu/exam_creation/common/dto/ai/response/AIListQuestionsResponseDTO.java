package com.wedu.exam_creation.common.dto.ai.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIListQuestionsResponseDTO {
    private List<AIQuestionResponseDTO> questions;
}
