package com.wedu.exam_creation.question.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionEntity {
    private String id;
    private String chapterId;
    private String lessonId;
    private String exerciseType;
    private String difficultyLevel;
    private List<String> learningOutcomes;
    private String questionType;

    @Builder.Default
    private ContentEntity question = new ContentEntity();

    @Builder.Default
    private List<ContentEntity> options = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}