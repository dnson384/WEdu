package com.fckedu.exam_creation.category.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEntity {
    private String id;
    private String subject;
    private String chapter;
    private List<LessonDataEntity> lessons;
}
