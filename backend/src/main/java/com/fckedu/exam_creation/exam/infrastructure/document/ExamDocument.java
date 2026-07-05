package com.fckedu.exam_creation.exam.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "Exams")
public class ExamDocument {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    @Field(targetType = FieldType.OBJECT_ID)
    private String draftId;

    private String name;
    private List<ChapterExamDocument> chapters;
    private List<QuestionExamDocument> questions;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
