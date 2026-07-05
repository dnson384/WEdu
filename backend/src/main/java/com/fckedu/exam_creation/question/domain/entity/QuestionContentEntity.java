package com.fckedu.exam_creation.question.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionContentEntity {
    private String template;

    @Builder.Default
    private VariablesEntity variables = new VariablesEntity();
}
