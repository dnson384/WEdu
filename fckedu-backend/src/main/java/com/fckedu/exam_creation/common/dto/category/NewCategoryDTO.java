package com.fckedu.exam_creation.common.dto.category;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewCategoryDTO {
    private String subject;
    private String chapter;
    private List<NewLessonDataDTO> lessons;
}
