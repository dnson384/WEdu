package com.fckedu.exam_creation.common.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewBankStatDTO {
    private String exerciseType;
    private List<String> difficultyLevels;
    private List<String> learningOutcomes;
    private String questionType;
    private int count;
}

