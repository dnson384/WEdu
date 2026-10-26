package com.wedu.exam_creation.questionReview.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegeneratedQuestionDocument {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String chapterId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String lessonId;

    private String exerciseType;

    private String difficultyLevel;

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    private String questionType;

    private String status;

    private ContentDocument question;

    @Builder.Default
    private List<ContentDocument> options = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    @LastModifiedDate
    private Instant updatedAt;
}
