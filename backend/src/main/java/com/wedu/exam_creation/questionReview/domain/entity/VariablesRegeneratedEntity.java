package com.wedu.exam_creation.questionReview.domain.entity;

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
public class VariablesRegeneratedEntity {
    @Builder.Default
    private Map<String, String> math = new HashMap<>();

    @Builder.Default
    private Map<String, String> image = new HashMap<>();
}
