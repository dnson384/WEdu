package com.wedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamEntity {
    private String id;
    private String userId;
    private String name;
    private List<ChapterExamEntity> chapters;
    private List<QuestionExamEntity> questions;
    private Instant createdAt;
    private Instant updatedAt;
}
