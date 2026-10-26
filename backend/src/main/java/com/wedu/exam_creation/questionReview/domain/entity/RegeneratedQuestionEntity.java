package com.wedu.exam_creation.questionReview.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratedQuestionEntity {
    private String id;
    private String chapterId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;
    private String status;

    @Builder.Default
    private ContentRegeneratedEntity question = new ContentRegeneratedEntity();

    @Builder.Default
    private List<ContentRegeneratedEntity> options = new ArrayList<>();

    private Instant createdAt;
    private Instant updatedAt;
}
