package com.wedu.exam_creation.exporter.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ExportMatrixRequestDTO {
    @NotBlank
    private String examId;
    @NotBlank
    private String examName;
}
