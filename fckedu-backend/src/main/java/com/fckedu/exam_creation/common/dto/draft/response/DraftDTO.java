package com.fckedu.exam_creation.common.dto.draft.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DraftDTO {
    private String id;
    private String userId;
    private String examName;
    private Integer questionsCount;
    private List<String> questionTypes;
    private List<ChapterDraftDTO> chapters;
}
