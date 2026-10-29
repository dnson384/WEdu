package com.wedu.exam_creation.moderator.dto;

import com.wedu.exam_creation.common.dto.question.response.ContentDTO;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class EditPendingQuestionDTO {
    private Instant updatedAt;
    private ContentDTO question;
    private List<ContentDTO> options;
}
