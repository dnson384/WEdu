package com.fckedu.exam_creation.chapter.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterEntity {
    private String id;
    private String subject;
    private String name;
    private List<LessonDataEntity> lessons;
}
