package com.wedu.exam_creation.draft.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
// Payload dùng chung cho update chương và bài
public class UpdateDraftDTO {
    List<UpdateParamDTO> add;
    List<String> del;
}
