package com.fckedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEntity {
    private String id;
    private String userId;
    private String draftId;
    private String name;
    private List<ChapterExamEntity> chapters;
    private List<QuestionExamEntity> questions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
