package com.fckedu.exam_creation.question.infrastructure.document;

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
public class VariablesDocument {
    @Builder.Default
    private Map<String, String> math = new HashMap<>();

    @Builder.Default
    private Map<String, String> image = new HashMap<>();
}
