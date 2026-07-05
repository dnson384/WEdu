package com.fckedu.exam_creation.common.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariablesDTO {
    @Builder.Default
    private Map<String, String> math = new HashMap<>();

    @Builder.Default
    private Map<String, String> image = new HashMap<>();
}
