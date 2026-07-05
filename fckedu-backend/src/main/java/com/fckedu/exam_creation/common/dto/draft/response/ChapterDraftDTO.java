package com.fckedu.exam_creation.common.dto.draft.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChapterDraftDTO {
    String id;
    String name;
    List<LessonDraftDTO> lessons;
}
