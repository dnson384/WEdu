package com.fckedu.exam_creation.question.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Questions")
@CompoundIndex(name = "chapter_lesson_idx", def = "{'chapterId': 1, 'lessonId': 1}", unique = true)
public class QuestionDocument {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String categoryId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String lessonId;

    @Indexed
    private String exerciseType;

    @Indexed
    private String difficultyLevel;

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    @Indexed
    private String questionType;

    private QuestionContentDocument question;

    @Builder.Default
    private List<OptionDataDocument> options = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
