package com.fckedu.exam_creation.question.infrastructure.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionDataDocument {
    private String template;

    @Builder.Default
    private VariablesDocument variables = new VariablesDocument();
}
