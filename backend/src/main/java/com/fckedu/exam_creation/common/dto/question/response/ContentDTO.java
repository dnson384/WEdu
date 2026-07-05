package com.fckedu.exam_creation.common.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentDTO {
    private String template;

    @Builder.Default
    private VariablesDTO variables = new VariablesDTO();
}
