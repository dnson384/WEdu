package com.wedu.exam_creation.questionReview.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRegeneratedEntity {
    private String template;

    @Builder.Default
    private VariablesRegeneratedEntity variables = new VariablesRegeneratedEntity();
}
