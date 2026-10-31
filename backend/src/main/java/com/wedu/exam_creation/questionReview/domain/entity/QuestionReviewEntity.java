package com.wedu.exam_creation.questionReview.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionReviewEntity {
    private String id;
    private int regenerationCount;
    private long revision;

    @Builder.Default
    private List<RegeneratedQuestionEntity> regeneratedQuestions = new ArrayList<>();
}
