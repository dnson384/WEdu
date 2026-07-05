package com.fckedu.exam_creation.draft.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChaptersDraftDTO {
    String draftId;
    List<UpdateParamDTO> add;
    List<String> del;
}
