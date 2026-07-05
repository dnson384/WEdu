package com.fckedu.exam_creation.draft.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDraftEntity {
    String id;
    String name;
    List<LessonDraftEntity> lessons;
}
