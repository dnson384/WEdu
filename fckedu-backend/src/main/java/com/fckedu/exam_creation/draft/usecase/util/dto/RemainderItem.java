package com.fckedu.exam_creation.draft.usecase.util.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemainderItem {
    private String level;
    private Double remainder;
}
