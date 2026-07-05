package com.fckedu.exam_creation.common.dto.category.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedCategoryResponse {
    private String chapterId;
    private String lessonId;
}
