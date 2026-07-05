package com.fckedu.exam_creation.common.dto.question;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewQuestionContentDTO {
    private String template;

    @Builder.Default
    private NewVariablesDTO variables = new NewVariablesDTO();

}
