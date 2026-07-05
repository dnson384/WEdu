package com.fckedu.exam_creation.exam.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExamEntity {
    private String questionType;
    private String difficultyLevel;
    private List<String> questionIds;
}