package com.wedu.exam_creation.moderator.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class ReviewPendingQuestionDTO {
    private Instant updatedAt;
    private Long reviewRevision;
}
