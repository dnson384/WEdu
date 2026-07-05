package com.fckedu.exam_creation.category.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDataEntity {
    private String id;
    private String name;

    @Builder.Default
    private List<BankStatEntity> bankStats = new ArrayList<>();
}
