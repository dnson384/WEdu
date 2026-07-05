package com.fckedu.exam_creation.common.dto.category.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDataResponseDTO {
    private String id;
    private String name;

    @Builder.Default
    private List<BankStatResponseDTO> bankStats = new ArrayList<>();
}
