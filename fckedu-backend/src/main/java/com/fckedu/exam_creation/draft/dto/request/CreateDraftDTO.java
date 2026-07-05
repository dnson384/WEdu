package com.fckedu.exam_creation.draft.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateDraftDTO {
    private Integer questionsCount;
    private String examName;
    private List<String> questionTypes;
}
