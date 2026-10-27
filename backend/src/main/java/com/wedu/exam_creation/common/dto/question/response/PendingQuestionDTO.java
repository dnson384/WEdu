package com.wedu.exam_creation.common.dto.question.response;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class PendingQuestionDTO extends QuestionDTO {
    private String chapter;
    private String lesson;
    private String status;
    private Instant createdAt;
}
