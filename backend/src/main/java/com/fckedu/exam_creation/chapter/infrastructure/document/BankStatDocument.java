package com.fckedu.exam_creation.category.infrastructure.document;

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
public class BankStatDocument {
    private String exerciseType;

    @Builder.Default
    private List<String> difficultyLevels = new ArrayList<>();

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();
    private String questionType;

    @Builder.Default
    private Integer count = 0;
}
