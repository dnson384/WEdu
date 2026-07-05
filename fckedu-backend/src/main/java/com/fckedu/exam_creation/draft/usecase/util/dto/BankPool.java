package com.fckedu.exam_creation.draft.usecase.util.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankPool {
    private String chapterId;
    private String lessonId;
    private String type;
    private String level;
    private Integer available;
}
