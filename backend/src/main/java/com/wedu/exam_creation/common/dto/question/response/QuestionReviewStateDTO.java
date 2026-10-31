package com.wedu.exam_creation.common.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewStateDTO {
    private int regenerationCount;
    private long revision;
    private QuestionDTO latestQuestion;
}
