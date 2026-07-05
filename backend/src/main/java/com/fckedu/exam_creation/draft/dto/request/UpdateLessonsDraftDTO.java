package com.fckedu.exam_creation.draft.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLessonsDraftDTO {
    private String draftId;
    private String chapterId;
    private List<UpdateParamDTO> add;
    private List<String> del;
}
