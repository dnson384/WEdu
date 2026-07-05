package com.fckedu.exam_creation.question.domain.entity;

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
public class QuestionEntity {
    private String id;
    private String categoryId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;

    @Builder.Default
    private QuestionContentEntity question = new QuestionContentEntity();

    @Builder.Default
    private List<OptionDataEntity> options = new ArrayList<>();
}