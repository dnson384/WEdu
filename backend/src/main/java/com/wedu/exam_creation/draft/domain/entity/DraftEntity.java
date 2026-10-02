package com.wedu.exam_creation.draft.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftEntity {
    private String id;
    private String userId;
    private String examName;
    private Integer questionsCount;
    private List<String> questionTypes;
    private List<ChapterDraftEntity> chapters;
    private Instant createdAt;
    private Instant updatedAt;
}
