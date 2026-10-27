package com.wedu.exam_creation.common.dto.question.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingQuestionPageDTO {
    private List<PendingQuestionDTO> items;
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;
}
