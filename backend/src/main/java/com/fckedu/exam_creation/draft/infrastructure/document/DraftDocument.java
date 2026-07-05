package com.fckedu.exam_creation.draft.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
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
@Document(collection = "Drafts")
public class DraftDocument {
    @Id
    private String id;

    @Field(targetType = FieldType.OBJECT_ID)
    private String userId;

    private String examName;
    private Integer questionsCount;

    @Builder.Default
    private List<String> questionTypes = new ArrayList<>();

    @Builder.Default
    private List<ChapterDraftDocument> chapters = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
