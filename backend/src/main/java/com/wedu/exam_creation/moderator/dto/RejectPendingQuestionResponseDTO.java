package com.wedu.exam_creation.moderator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RejectPendingQuestionResponseDTO {
    private String status;
    private int regenerationCount;
}
