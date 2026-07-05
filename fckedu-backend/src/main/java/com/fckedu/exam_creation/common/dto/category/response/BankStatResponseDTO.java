package com.fckedu.exam_creation.common.dto.category.response;


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
public class BankStatResponseDTO {
    private String exerciseType;

    @Builder.Default
    private List<String> difficultyLevels = new ArrayList<>();

    @Builder.Default
    private List<String> learningOutcomes = new ArrayList<>();

    private String questionType;
    private Integer count;
}
