package com.fckedu.exam_creation.question.domain.entity;

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
public class VariablesEntity {
    @Builder.Default
    private Map<String, String> math = new HashMap<>();

    @Builder.Default
    private Map<String, String> image = new HashMap<>();
}
