package com.fckedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterExamEntity {
    List<String> lessonIds;
    private String id;
}
