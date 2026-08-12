package com.fckedu.exam_creation.common.dto.chapter.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SavedChapterResponse {
    private String chapterId;
    private String lessonId;
}
